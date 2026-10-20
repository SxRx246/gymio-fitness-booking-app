package com.ga.gymio.service;

import com.ga.gymio.model.AuditLog;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Handles the creation and storage of audit log entries.
 */
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
