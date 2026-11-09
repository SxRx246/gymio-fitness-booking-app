package com.ga.user_service.service;

import com.ga.user_service.model.AuditLog;
import com.ga.user_service.model.User;
import com.ga.user_service.repository.AuditLogRepository;
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
     * @param user the user who performed the action
     */
    public void log(
            AuditLog.Action action,
            String description,
            User user
    ) {
        AuditLog auditLog = new AuditLog();

        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setUser(user);

        auditLogRepository.save(auditLog);
    }
}
