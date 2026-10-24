package com.ga.gymio.service;

import com.ga.gymio.repository.MembershipPlanRepository;
import com.ga.gymio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMembershipService {

    private final UserRepository userRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final AuditLogService auditLogService;

}
