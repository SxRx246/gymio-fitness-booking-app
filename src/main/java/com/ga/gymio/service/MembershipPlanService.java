package com.ga.gymio.service;

import com.ga.gymio.authentication.MyUserDetails;
import com.ga.gymio.dto.request.MembershipPlanRequest;
import com.ga.gymio.dto.response.MembershipResponse;
import com.ga.gymio.exception.ForbiddenException;
import com.ga.gymio.exception.InformationExistsException;
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

@Service
@RequiredArgsConstructor
public class MembershipPlanService {
    private final MembershipPlanRepository membershipPlanRepository;
    private static final Logger logger = LoggerFactory.getLogger(MembershipPlanService.class);

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
                membershipPlan.getCreatedAt(),
                membershipPlan.getUpdatedAt()
        );
    }
}
