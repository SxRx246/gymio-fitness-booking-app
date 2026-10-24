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

@RestController
@RequestMapping("/api/membership-plans")
@RequiredArgsConstructor
@Validated
public class MembershipPlanController {
    private final MembershipPlanService membershipPlanService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MembershipResponse createMembershipPlan(
            @Valid @RequestBody MembershipPlanRequest request) {

        return membershipPlanService.createMembershipPlan(request);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MembershipResponse> getAllMembershipPlans() {

        return membershipPlanService.getAllMembershipPlans();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MembershipResponse getMembershipPlan(
            @PathVariable
            @Positive(message = "Membership plan ID must be positive")
                    Long id) {

        return membershipPlanService.getMembershipPlan(id);
    }

}
