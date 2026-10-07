package com.ga.gymio.service;

import com.ga.gymio.model.AuditLog;
import com.ga.gymio.model.User;
import com.ga.gymio.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

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
