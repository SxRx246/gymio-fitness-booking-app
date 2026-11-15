package com.ga.membership_payment_service.service;

import com.ga.membership_payment_service.model.AuditLog;
import com.ga.membership_payment_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Records an action performed by a user.
     *
     * @param action the type of action performed
     * @param description details about the action
     * @param userId the ID of the user who performed the action
     */
    public void log(
            AuditLog.Action action,
            String description,
            Long userId
    ) {
        AuditLog auditLog = new AuditLog();

        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setUserId(userId);

        auditLogRepository.save(auditLog);
    }
}
