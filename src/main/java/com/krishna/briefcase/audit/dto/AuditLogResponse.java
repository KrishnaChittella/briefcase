package com.krishna.briefcase.audit.dto;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String entityType,
        Long entityId,
        String action,
        String performedBy,
        Instant occurredAt,
        String details
) {
}
