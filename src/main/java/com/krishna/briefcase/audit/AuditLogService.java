package com.krishna.briefcase.audit;

import com.krishna.briefcase.audit.dto.AuditLogResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public List<AuditLogResponse> findForCase(Long caseId) {
        return repository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc("CASE", caseId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getEntityType(), log.getEntityId(), log.getAction(),
                log.getPerformedBy(), log.getOccurredAt(), log.getDetails());
    }
}
