package com.ga.gymio.controller;

import com.ga.gymio.dto.request.MembershipPlanRequest;
import com.ga.gymio.dto.response.MembershipResponse;
import com.ga.gymio.service.MembershipPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membership-plans")
@RequiredArgsConstructor
@Validated
public class MembershipPlanController {
    private final MembershipPlanService membershipPlanService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MembershipResponse> createMembershipPlan(
            @Valid @RequestBody MembershipPlanRequest request) {

        MembershipResponse response =
                membershipPlanService.createMembershipPlan(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
