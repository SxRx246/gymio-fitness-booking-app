package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.request.MembershipPlanRequest;
import com.ga.gymio.dto.response.MembershipResponse;
import com.ga.gymio.exception.ConflictException;
import com.ga.gymio.exception.ForbiddenException;
import com.ga.gymio.exception.InformationExistsException;
import com.ga.gymio.exception.InformationNotFoundException;
import com.ga.gymio.model.MembershipPlan;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.MembershipPlanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages membership plan creation, retrieval, updates, and deactivation.
 */
@Service
@RequiredArgsConstructor
public class MembershipPlanService {
    private final MembershipPlanRepository membershipPlanRepository;
    private static final Logger logger = LoggerFactory.getLogger(MembershipPlanService.class);

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

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException(
                    "Only administrators can create membership plans"
            );
        }

        if (membershipPlanRepository.existsByName(request.getName())) {
            throw new InformationExistsException(
                    "A membership plan with this name already exists"
            );

        }

        MembershipPlan membershipPlan = new MembershipPlan();
        membershipPlan.setName(request.getName());
        membershipPlan.setDurationByDays(request.getDurationByDays());
        membershipPlan.setPrice(request.getPrice());


        MembershipPlan savedPlan = membershipPlanRepository.save(membershipPlan);

        logger.info("Membership plan created successfully with id {}", savedPlan.getId());

        return mapToResponse(savedPlan);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ((MyUserDetails) authentication.getPrincipal()).getUser();
    }

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
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == User.Role.ADMIN) {
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
        User currentUser = getCurrentUser();

        logger.info("Retrieving membership plan with id {}", id);

        MembershipPlan membershipPlan = membershipPlanRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Membership plan with id " + id + " not found"
                        )
                );

        if (currentUser.getRole() != User.Role.ADMIN
                && !membershipPlan.isActive()) {
            throw new InformationNotFoundException(
                    "Membership plan with id " + id + " not found"
            );
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

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException(
                    "Only administrators can update membership plans"
            );
        }

        MembershipPlan membershipPlan = membershipPlanRepository.findById(id)
                .orElseThrow(() ->
                        new InformationNotFoundException(
                                "Membership plan with id " + id + " not found"
                        )
                );

        if (!membershipPlan.isActive()) {
            throw new ConflictException(
                    "Cannot update an inactive membership plan"
            );
        }

        if (!membershipPlan.getName().equals(request.getName())
                && membershipPlanRepository.existsByName(request.getName())) {
            throw new InformationExistsException(
                    "A membership plan with this name already exists"
            );
        }

        membershipPlan.setName(request.getName());
        membershipPlan.setDurationByDays(request.getDurationByDays());
        membershipPlan.setPrice(request.getPrice());

        MembershipPlan updatedPlan = membershipPlanRepository.save(membershipPlan);

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

        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException(
                    "Only administrators can deactivate membership plans"
            );
        }

        MembershipPlan membershipPlan = membershipPlanRepository.findById(id)
                .orElseThrow(() -> new InformationNotFoundException(
                        "Membership plan with id " + id + " not found"
                ));

        if (!membershipPlan.isActive()) {
            throw new ConflictException(
                    "Membership plan is already inactive"
            );
        }

        membershipPlan.setActive(false);
        membershipPlanRepository.save(membershipPlan);

        logger.info("Membership plan deactivated successfully with id {}", id);
    }

}
