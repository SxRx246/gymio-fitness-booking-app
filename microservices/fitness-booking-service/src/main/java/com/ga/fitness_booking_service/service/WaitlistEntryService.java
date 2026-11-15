package com.ga.fitness_booking_service.service;

import com.ga.fitness_booking_service.dto.AuthenticatedUser;
import com.ga.fitness_booking_service.dto.request.WaitlistEntryRequest;
import com.ga.fitness_booking_service.dto.response.WaitlistEntryResponse;
import com.ga.fitness_booking_service.event.BookingConfirmedEvent;
import com.ga.fitness_booking_service.event.WaitlistOfferCreatedEvent;
import com.ga.fitness_booking_service.event.WaitlistOfferExpiredEvent;
import com.ga.fitness_booking_service.exception.InformationNotFoundException;
import com.ga.fitness_booking_service.model.*;
import com.ga.fitness_booking_service.model.WaitlistEntry.Status;
import com.ga.fitness_booking_service.repository.BookingRepository;
import com.ga.fitness_booking_service.repository.FitnessClassRepository;
import com.ga.fitness_booking_service.repository.WaitlistEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages customer waitlist entries for fitness classes.
 * Handles joining and leaving the waitlist, issuing time-limited offers,
 * confirming bookings when offers are accepted, and publishing notification events.
 */
@Service
@RequiredArgsConstructor
public class WaitlistEntryService {

    private static final int OFFER_MINUTES = 30;

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final FitnessClassRepository fitnessClassRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    /**
     * Adds the authenticated customer to a fitness class waitlist.
     * The class must be scheduled, not yet started, and have no available places.
     * Existing cancelled entries are reactivated instead of creating duplicates.
     *
     * @param request the request containing the fitness class ID
     * @return the saved waitlist entry response
     * @throws IllegalArgumentException if the fitness class cannot be found
     * @throws IllegalStateException if the class is unavailable, its capacity is
     *                               invalid, a place is available, or the customer
     *                               already has an active waitlist entry
     */
    @Transactional
    public WaitlistEntryResponse joinWaitlist(WaitlistEntryRequest request) {

        AuthenticatedUser customer = getCurrentCustomer();

        FitnessClass fitnessClass = getLockedClass(
                request.getFitnessClassId()
        );

        validateFutureScheduledClass(fitnessClass);

        long confirmed = bookingRepository.countByFitnessClassIdAndStatus(
                fitnessClass.getId(),
                Booking.Status.CONFIRMED
        );

        long activeOffers = countActiveOffers(fitnessClass.getId());

        if (fitnessClass.getCapacity() == null
                || fitnessClass.getCapacity() <= 0) {
            throw new IllegalStateException(
                    "This fitness class has an invalid capacity."
            );
        }

        if (confirmed + activeOffers < fitnessClass.getCapacity()) {
            throw new IllegalStateException(
                    "This class has an available place. Please book it directly."
            );
        }

        var existing = waitlistEntryRepository
                .findByCustomerIdAndFitnessClass_Id(
                        customer.userId(),
                        fitnessClass.getId()
                );

        if (existing.isPresent()) {
            WaitlistEntry entry = existing.get();

            if (entry.getStatus() != Status.CANCELLED) {
                throw new IllegalStateException(
                        "You already have an active waitlist entry for this class."
                );
            }

            entry.setStatus(Status.WAITING);
            entry.setJoinedAt(LocalDateTime.now());
            entry.setNotifiedAt(null);
            entry.setOfferExpiresAt(null);

            return toResponse(waitlistEntryRepository.save(entry));
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setCustomerId(customer.userId());
        entry.setFitnessClass(fitnessClass);
        entry.setStatus(Status.WAITING);

        return toResponse(waitlistEntryRepository.save(entry));
    }

    /**
     * Retrieves the authenticated customer's waitlist entries, ordered from
     * newest to oldest.
     *
     * @return the customer's waitlist entries
     */
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getMyWaitlist() {

        AuthenticatedUser customer = getCurrentCustomer();

        return waitlistEntryRepository
                .findAllByCustomerIdOrderByJoinedAtDesc(customer.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Cancels the authenticated customer's waitlist entry.
     * If the customer leaves an active offer, the next waiting customer is
     * considered for an available place.
     *
     * @param entryId the ID of the waitlist entry to cancel
     * @throws InformationNotFoundException if the entry cannot be found for
     *                                      the authenticated customer
     * @throws IllegalStateException if the entry cannot be cancelled
     */
    @Transactional
    public void leaveWaitlist(Long entryId) {

        AuthenticatedUser customer = getCurrentCustomer();

        WaitlistEntry entry = waitlistEntryRepository
                .findByIdAndCustomerId(entryId, customer.userId())
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Waitlist entry not found."
                        )
                );

        if (entry.getStatus() != Status.WAITING
                && entry.getStatus() != Status.NOTIFIED) {
            throw new IllegalStateException(
                    "This waitlist entry cannot be cancelled."
            );
        }

        Long classId = entry.getFitnessClass().getId();

        entry.setStatus(Status.CANCELLED);
        entry.setOfferExpiresAt(null);
        waitlistEntryRepository.save(entry);

        notifyNextWaitingCustomer(classId);
    }

    /**
     * Accepts an active waitlist offer and creates a confirmed booking.
     * Validates the offer's expiration, class availability, and existing bookings.
     * On success, marks the waitlist entry as promoted and records the action
     * in the audit log before publishing a booking confirmation event.
     *
     * @param entryId the ID of the waitlist entry whose offer is being accepted
     * @return the updated waitlist entry response
     * @throws InformationNotFoundException if the entry cannot be found for
     *                                      the authenticated customer
     * @throws IllegalStateException if the offer is unavailable or expired,
     *                               the customer already has a booking, or
     *                               no place is available
     */
    @Transactional(noRollbackFor = IllegalStateException.class)
    public WaitlistEntryResponse acceptOffer(Long entryId) {

        AuthenticatedUser customer = getCurrentCustomer();

        WaitlistEntry entry = waitlistEntryRepository
                .findByIdAndCustomerId(entryId, customer.userId())
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Waitlist entry not found."
                        )
                );

        Long classId = entry.getFitnessClass().getId();

        FitnessClass fitnessClass = getLockedClass(classId);

      entry = waitlistEntryRepository
                .findByIdAndCustomerId(entryId, customer.userId())
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Waitlist entry not found."
                        )
                );

        if (entry.getStatus() != Status.NOTIFIED) {
            throw new IllegalStateException(
                    "There is no active offer for this waitlist entry."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (entry.getOfferExpiresAt() == null
                || !entry.getOfferExpiresAt().isAfter(now)) {

            entry.setStatus(Status.CANCELLED);
            entry.setOfferExpiresAt(null);
            waitlistEntryRepository.save(entry);

            notifyNextWaitingCustomer(classId);

            throw new IllegalStateException(
                    "This offer has expired. Please check your waitlist."
            );
        }

        validateFutureScheduledClass(fitnessClass);

        boolean alreadyBooked =
                bookingRepository.existsByCustomerIdAndFitnessClassIdAndStatus(
                        customer.userId(),
                        classId,
                        Booking.Status.CONFIRMED
                );

        if (alreadyBooked) {
            entry.setStatus(Status.CANCELLED);
            entry.setOfferExpiresAt(null);
            waitlistEntryRepository.save(entry);

            notifyNextWaitingCustomer(classId);

            throw new IllegalStateException(
                    "You already have a booking for this class."
            );
        }

        long confirmed = bookingRepository.countByFitnessClassIdAndStatus(
                classId,
                Booking.Status.CONFIRMED
        );

        if (fitnessClass.getCapacity() == null
                || confirmed >= fitnessClass.getCapacity()) {
            throw new IllegalStateException(
                    "No place is currently available for this class."
            );
        }

        Booking booking = new Booking();
        booking.setCustomerId(customer.userId());
        booking.setFitnessClass(fitnessClass);
        booking.setStatus(Booking.Status.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        entry.setStatus(Status.PROMOTED);
        entry.setOfferExpiresAt(null);
        waitlistEntryRepository.save(entry);

        auditLogService.log(
                AuditLog.Action.BOOK_CLASS,
                "User " + customer.userId()
                        + " accepted a waitlist offer and booked Fitness Class "
                        + classId,
                customer.userId()
        );

        eventPublisher.publishEvent(
                new BookingConfirmedEvent(
                        customer.userId(),
                        customer.email(),
                        savedBooking.getId(),
                        fitnessClass.getName(),
                        fitnessClass.getStartTime().toString(),
                        fitnessClass.getEndTime().toString()
                )
        );

        return toResponse(entry);
    }

    /**
     * Declines an active waitlist offer for the authenticated customer.
     *
     * <p>The offer is marked as cancelled, its expiration time is cleared,
     * and the next eligible waiting customer is considered for the place.</p>
     *
     * @param entryId the ID of the waitlist entry associated with the offer
     * @return the response representing the cancelled waitlist entry
     * @throws InformationNotFoundException if the waitlist entry does not exist
     *         or does not belong to the authenticated customer
     * @throws IllegalStateException if the entry does not have an active offer
     */
    @Transactional
    public WaitlistEntryResponse declineOffer(Long entryId) {

        AuthenticatedUser customer = getCurrentCustomer();

        WaitlistEntry entry = waitlistEntryRepository
                .findByIdAndCustomerId(entryId, customer.userId())
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Waitlist entry not found."
                        )
                );

        Long classId = entry.getFitnessClass().getId();

        getLockedClass(classId);

        entry = waitlistEntryRepository
                .findByIdAndCustomerId(entryId, customer.userId())
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Waitlist entry not found."
                        )
                );

        if (entry.getStatus() != Status.NOTIFIED) {
            throw new IllegalStateException(
                    "There is no active offer to decline."
            );
        }

        entry.setStatus(Status.CANCELLED);
        entry.setOfferExpiresAt(null);

        WaitlistEntry saved = waitlistEntryRepository.save(entry);

        notifyNextWaitingCustomer(classId);

        return toResponse(saved);
    }

    /**
     * Counts the active offers reserving places in a fitness class.
     *
     * <p>Only offers with a {@code NOTIFIED} status and an expiration time
     * later than the current time are counted.</p>
     *
     * @param fitnessClassId the ID of the fitness class
     * @return the number of unexpired offers for the specified class
     */
    @Transactional(readOnly = true)
    public long countActiveOffers(Long fitnessClassId) {

        return waitlistEntryRepository
                .countByFitnessClass_IdAndStatusAndOfferExpiresAtAfter(
                        fitnessClassId,
                        Status.NOTIFIED,
                        LocalDateTime.now()
                );
    }

    /**
     * Sends an offer to the next eligible customer in the waitlist.
     *
     * <p>The method checks that the fitness class is scheduled, has not started,
     * and has a defined capacity. It then accounts for confirmed bookings and
     * active offers before selecting the earliest waiting entry.</p>
     *
     * <p>The selected entry is marked as {@code NOTIFIED}, and an offer expiration
     * time is set using {@link #OFFER_MINUTES}. A notification event is published
     * after the transaction commits.</p>
     *
     * @param fitnessClassId the ID of the fitness class
     */
    @Transactional
    public void notifyNextWaitingCustomer(Long fitnessClassId) {

        FitnessClass fitnessClass;

        try {
            fitnessClass = getLockedClass(fitnessClassId);
        } catch (InformationNotFoundException exception) {
            return;
        }

        if (fitnessClass.getStatus() != FitnessClass.Status.SCHEDULED
                || fitnessClass.getStartTime() == null
                || !fitnessClass.getStartTime().isAfter(LocalDateTime.now())
                || fitnessClass.getCapacity() == null) {
            return;
        }

        long confirmed = bookingRepository.countByFitnessClassIdAndStatus(
                fitnessClassId,
                Booking.Status.CONFIRMED
        );

        long activeOffers = countActiveOffers(fitnessClassId);

        if (confirmed + activeOffers >= fitnessClass.getCapacity()) {
            return;
        }

        List<WaitlistEntry> waitingEntries = waitlistEntryRepository
                .findAllByFitnessClass_IdAndStatusOrderByJoinedAtAsc(
                        fitnessClassId,
                        Status.WAITING
                );

        if (waitingEntries.isEmpty()) {
            return;
        }

        WaitlistEntry nextEntry = waitingEntries.get(0);
        LocalDateTime now = LocalDateTime.now();

        nextEntry.setStatus(Status.NOTIFIED);
        nextEntry.setNotifiedAt(now);
        nextEntry.setOfferExpiresAt(now.plusMinutes(OFFER_MINUTES));

        waitlistEntryRepository.save(nextEntry);

        eventPublisher.publishEvent(
                new WaitlistOfferCreatedEvent(
                        nextEntry.getCustomerId(),
                        fitnessClass.getName(),
                        OFFER_MINUTES
                )
        );

    }

    /**
     * Expires outstanding waitlist offers and considers the next eligible
     * customers for the affected classes.
     *
     * <p>The method finds notified entries whose offer expiration times have passed,
     * locks the associated fitness classes, and rechecks each offer before
     * cancelling it. Expiration notification events are published after commit.</p>
     *
     * <p>This method is intended to be invoked periodically by
     * {@code WaitlistOfferExpiryScheduler}.</p>
     */
    @Transactional
    public void expireOffersAndNotifyNext() {

        LocalDateTime now = LocalDateTime.now();

        List<WaitlistEntry> expiredEntries =
                waitlistEntryRepository
                        .findAllByStatusAndOfferExpiresAtLessThanEqual(
                                Status.NOTIFIED,
                                now
                        );

        Set<Long> affectedClassIds = new LinkedHashSet<>();

        for (WaitlistEntry entry : expiredEntries) {
            Long classId = entry.getFitnessClass().getId();

            FitnessClass fitnessClass = getLockedClass(classId);

            if (entry.getStatus() == Status.NOTIFIED
                    && entry.getOfferExpiresAt() != null
                    && !entry.getOfferExpiresAt().isAfter(LocalDateTime.now())) {

                entry.setStatus(Status.CANCELLED);
                entry.setOfferExpiresAt(null);
                waitlistEntryRepository.save(entry);

                eventPublisher.publishEvent(
                        new WaitlistOfferExpiredEvent(
                                entry.getCustomerId(),
                                fitnessClass.getName()
                        )
                );

                affectedClassIds.add(classId);
            }
        }

        for (Long classId : affectedClassIds) {
            notifyNextWaitingCustomer(classId);
        }
    }

    /**
     * Retrieves a fitness class while acquiring a database-level write lock.
     *
     * <p>The lock helps coordinate concurrent operations involving the same
     * fitness class, such as accepting offers and checking available capacity.</p>
     *
     * @param classId the ID of the fitness class
     * @return the locked fitness class
     * @throws InformationNotFoundException if the fitness class does not exist
     */
    private FitnessClass getLockedClass(Long classId) {

        return fitnessClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Fitness class with id "
                                        + classId
                                        + " not found."
                        )
                );
    }

    /**
     * Validates that a fitness class is scheduled and has not yet started.
     *
     * @param fitnessClass the fitness class to validate
     * @throws IllegalStateException if the class is not scheduled or its start
     *         time is missing or has already passed
     */
    private void validateFutureScheduledClass(FitnessClass fitnessClass) {

        if (fitnessClass.getStatus() != FitnessClass.Status.SCHEDULED) {
            throw new IllegalStateException(
                    "This fitness class is not available."
            );
        }

        if (fitnessClass.getStartTime() == null
                || !fitnessClass.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "This fitness class has already started."
            );
        }
    }

    /**
     * Retrieves the authenticated user and verifies that the user has the
     * customer role.
     *
     * @return the authenticated customer identity
     * @throws IllegalStateException if the user is not authenticated,
     *         the principal is invalid, or the user does not have the customer role
     */
    private AuthenticatedUser getCurrentCustomer() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUser customer)) {
            throw new IllegalStateException(
                    "An authenticated customer is required."
            );
        }

        if (!"CUSTOMER".equalsIgnoreCase(customer.role())) {
            throw new IllegalStateException(
                    "Only customers can manage their waitlist."
            );
        }

        return customer;
    }

    /**
     * Converts a waitlist entry entity into its response DTO.
     *
     * <p>The response includes the entry ID, fitness class ID, status,
     * join timestamp, notification timestamp, and offer expiration time.</p>
     *
     * @param entry the waitlist entry to convert
     * @return the corresponding waitlist entry response
     */
    private WaitlistEntryResponse toResponse(WaitlistEntry entry) {

        return new WaitlistEntryResponse(
                entry.getId(),
                entry.getFitnessClass().getId(),
                entry.getStatus(),
                entry.getJoinedAt(),
                entry.getNotifiedAt(),
                entry.getOfferExpiresAt()
        );
    }
}
