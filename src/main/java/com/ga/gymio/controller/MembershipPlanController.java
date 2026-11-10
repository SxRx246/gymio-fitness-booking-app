package com.ga.gymio.controller;

import com.ga.gymio.dto.request.MembershipPlanRequest;
import com.ga.gymio.dto.response.MembershipResponse;
import com.ga.gymio.service.MembershipPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing membership plans.
 *
 * Provides endpoints to create, retrieve, update,
 * and deactivate membership plans.
 *
 * Business logic and authorization checks are handled
 * by the service layer, with additional access control
 * enforced through Spring Security.
 */
@RestController
@RequestMapping("/membership-plans")
@RequiredArgsConstructor
@Validated
public class MembershipPlanController {
    private final MembershipPlanService membershipPlanService;

    /**
     * Creates a new membership plan.
     *
     * Only administrators can create membership plans.
     *
     * @param request validated membership plan details
     * @return the created membership plan
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipResponse createMembershipPlan(
            @Valid @RequestBody MembershipPlanRequest request) {

        return membershipPlanService.createMembershipPlan(request);
    }

    /**
     * Retrieves all membership plans available to the
     * authenticated user.
     *
     * Administrators can retrieve all plans, while other
     * authenticated users can retrieve active plans.
     *
     * @return a list of membership plans
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MembershipResponse> getAllMembershipPlans() {

        return membershipPlanService.getAllMembershipPlans();
    }

    /**
     * Retrieves a membership plan by its ID.
     *
     * Administrators can retrieve active or inactive plans.
     * Other authenticated users can retrieve active plans only.
     *
     * @param id the membership plan ID, which must be positive
     * @return the requested membership plan
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MembershipResponse getMembershipPlan(
            @PathVariable
            @Positive(message = "Membership plan ID must be positive")
                    Long id) {

        return membershipPlanService.getMembershipPlan(id);
    }

    /**
     * Updates an existing membership plan.
     *
     * Only administrators can update membership plans.
     *
     * @param id the ID of the membership plan to update
     * @param request validated updated membership plan details
     * @return the updated membership plan
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipResponse updateMembershipPlan(
            @PathVariable
            @Positive(message = "Membership plan ID must be positive")
                    Long id,
            @Valid @RequestBody MembershipPlanRequest request) {

        return membershipPlanService.updateMembershipPlan(id, request);
    }

    /**
     * Deactivates a membership plan without permanently
     * deleting it from the database.
     *
     * Only administrators can deactivate membership plans.
     *
     * @param id the ID of the membership plan to deactivate
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMembershipPlan(
            @PathVariable
            @Positive(message = "Membership plan ID must be positive")
                    Long id) {

        membershipPlanService.deleteMembershipPlan(id);
    }

    /**
     * Reactivates an inactive membership plan.
     *
     * @param id the ID of the membership plan to activate
     */
    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void activateMembershipPlan(
            @PathVariable
            @Positive(message = "Membership plan ID must be positive")
                    Long id) {

        membershipPlanService.activateMembershipPlan(id);
    }

}
