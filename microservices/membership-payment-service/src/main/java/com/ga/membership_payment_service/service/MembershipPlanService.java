package com.ga.membership_payment_service.service;

import com.ga.membership_payment_service.dto.request.MembershipPlanRequest;
import com.ga.membership_payment_service.dto.response.MembershipResponse;
import com.ga.membership_payment_service.exception.ConflictException;
import com.ga.membership_payment_service.exception.ForbiddenException;
import com.ga.membership_payment_service.exception.InformationExistsException;
import com.ga.membership_payment_service.exception.InformationNotFoundException;
import com.ga.membership_payment_service.model.AuditLog;
import com.ga.membership_payment_service.model.MembershipPlan;
import com.ga.membership_payment_service.repository.MembershipPlanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manages membership plan creation, retrieval, updates, activation, and deactivation.
 */
@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final AuditLogService auditLogService;

    private static final Logger logger =
            LoggerFactory.getLogger(MembershipPlanService.class);

    /**
     * Creates a membership plan after checking administrator permissions
     * and ensuring its name is unique.
     *
     * @param request the membership plan details
     * @return the created membership plan
     */
    @Transactional
    public MembershipResponse createMembershipPlan(MembershipPlanRequest request) {

        logger.info("Creating Membership Plan: {}", request.getName());

        requireAdmin();

        Long currentUserId = getCurrentUserId();

        if (membershipPlanRepository.existsByName(request.getName())) {
            throw new InformationExistsException(
                    "A membership plan with this name already exists");
        }

        MembershipPlan membershipPlan = new MembershipPlan();
        membershipPlan.setName(request.getName());
        membershipPlan.setDurationByDays(request.getDurationByDays());
        membershipPlan.setPrice(request.getPrice());

        MembershipPlan savedPlan =
                membershipPlanRepository.save(membershipPlan);

        auditLogService.log(
                AuditLog.Action.CREATE_MEMBERSHIP_PLAN,
                "Admin " + currentUserId
                        + " created Membership Plan " + savedPlan.getId()
                        + " (name: " + savedPlan.getName() + ")",
                currentUserId
        );

        logger.info("Membership plan created successfully with id {}",
                savedPlan.getId());

        return mapToResponse(savedPlan);
    }

    /**
     * Retrieves the authenticated user's ID from the security context.
     *
     * @return the authenticated user's ID
     */
    private Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new ForbiddenException(
                    "User authentication is required.");
        }

        return userId;
    }

    /**
     * Checks whether the authenticated user has administrator permissions.
     *
     * @throws ForbiddenException if the user is not an administrator
     */
    private void requireAdmin() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ForbiddenException(
                    "Only administrators can perform this operation.");
        }
    }

    /**
     * Converts a membership plan entity into its response DTO.
     *
     * @param membershipPlan the membership plan entity
     * @return the membership plan response
     */
    private MembershipResponse mapToResponse(MembershipPlan membershipPlan) {

        return new MembershipResponse(
                membershipPlan.getId(),
                membershipPlan.getName(),
                membershipPlan.getDurationByDays(),
                membershipPlan.getPrice(),
                membershipPlan.isActive(),
                membershipPlan.getCreatedAt(),
                membershipPlan.getUpdatedAt()
        );
    }

    /**
     * Retrieves all membership plans for administrators and only active
     * plans for other authenticated users.
     *
     * @return the membership plans visible to the current user
     */
    @Transactional(readOnly = true)
    public List<MembershipResponse> getAllMembershipPlans() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            logger.info("Admin retrieving all membership plans");

            return membershipPlanRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        logger.info("Retrieving active membership plans");

        return membershipPlanRepository.findAllByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retrieves a membership plan by ID, hiding inactive plans from
     * non-administrator users.
     *
     * @param id the membership plan ID
     * @return the requested membership plan
     */
    @Transactional(readOnly = true)
    public MembershipResponse getMembershipPlan(Long id) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        logger.info("Retrieving membership plan with id {}", id);

        MembershipPlan membershipPlan =
                membershipPlanRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Membership plan with id " + id
                                                + " not found"));

        if (!isAdmin && !membershipPlan.isActive()) {
            throw new InformationNotFoundException(
                    "Membership plan with id " + id + " not found");
        }

        return mapToResponse(membershipPlan);
    }

    /**
     * Updates an active membership plan after checking administrator
     * permissions and ensuring its name remains unique.
     *
     * @param id the ID of the membership plan to update
     * @param request the updated membership plan details
     * @return the updated membership plan
     */
    @Transactional
    public MembershipResponse updateMembershipPlan(
            Long id, MembershipPlanRequest request) {

        logger.info("Updating membership plan with id {}", id);

        requireAdmin();

        Long currentUserId = getCurrentUserId();

        MembershipPlan membershipPlan =
                membershipPlanRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Membership plan with id " + id
                                                + " not found"));

        if (!membershipPlan.isActive()) {
            throw new ConflictException(
                    "Cannot update an inactive membership plan");
        }

        if (!membershipPlan.getName().equals(request.getName())
                && membershipPlanRepository.existsByName(request.getName())) {
            throw new InformationExistsException(
                    "A membership plan with this name already exists");
        }

        String oldName = membershipPlan.getName();
        Integer oldDuration = membershipPlan.getDurationByDays();
        BigDecimal oldPrice = membershipPlan.getPrice();

        membershipPlan.setName(request.getName());
        membershipPlan.setDurationByDays(request.getDurationByDays());
        membershipPlan.setPrice(request.getPrice());

        MembershipPlan updatedPlan =
                membershipPlanRepository.save(membershipPlan);

        auditLogService.log(
                AuditLog.Action.UPDATE_MEMBERSHIP_PLAN,
                "Admin " + currentUserId
                        + " updated Membership Plan " + updatedPlan.getId()
                        + ". Previous values: name=" + oldName
                        + ", duration=" + oldDuration
                        + ", price=" + oldPrice
                        + ". New values: name=" + updatedPlan.getName()
                        + ", duration=" + updatedPlan.getDurationByDays()
                        + ", price=" + updatedPlan.getPrice(),
                currentUserId
        );

        logger.info("Membership plan updated successfully with id {}",
                updatedPlan.getId());

        return mapToResponse(updatedPlan);
    }

    /**
     * Deactivates a membership plan without physically deleting it.
     *
     * @param id the ID of the membership plan to deactivate
     */
    @Transactional
    public void deleteMembershipPlan(Long id) {

        logger.info("Deactivating membership plan with id {}", id);

        requireAdmin();

        Long currentUserId = getCurrentUserId();

        MembershipPlan membershipPlan =
                membershipPlanRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Membership plan with id " + id
                                                + " not found"));

        if (!membershipPlan.isActive()) {
            throw new ConflictException(
                    "Membership plan is already inactive");
        }

        membershipPlan.setActive(false);
        membershipPlanRepository.save(membershipPlan);

        auditLogService.log(
                AuditLog.Action.DEACTIVATE_MEMBERSHIP_PLAN,
                "Admin " + currentUserId
                        + " deactivated Membership Plan "
                        + membershipPlan.getId()
                        + " (name: " + membershipPlan.getName() + ")",
                currentUserId
        );

        logger.info("Membership plan deactivated successfully with id {}",
                id);
    }

    /**
     * Reactivates an inactive membership plan after checking administrator
     * permissions.
     *
     * @param id the ID of the membership plan to activate
     */
    @Transactional
    public void activateMembershipPlan(Long id) {

        logger.info("Activating membership plan with id {}", id);

        requireAdmin();

        Long currentUserId = getCurrentUserId();

        MembershipPlan membershipPlan =
                membershipPlanRepository.findById(id)
                        .orElseThrow(() ->
                                new InformationNotFoundException(
                                        "Membership plan with id " + id
                                                + " not found"));

        if (membershipPlan.isActive()) {
            throw new ConflictException(
                    "Membership plan is already active");
        }

        membershipPlan.setActive(true);
        membershipPlanRepository.save(membershipPlan);

        auditLogService.log(
                AuditLog.Action.ACTIVATE_MEMBERSHIP_PLAN,
                "Admin " + currentUserId
                        + " activated Membership Plan "
                        + membershipPlan.getId()
                        + " (name: " + membershipPlan.getName() + ")",
                currentUserId
        );

        logger.info(
                "Membership plan activated successfully with id {}",
                membershipPlan.getId()
        );
    }
}
