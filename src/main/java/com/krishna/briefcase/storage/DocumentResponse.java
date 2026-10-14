package com.krishna.briefcase.storage;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        Long caseId,
        String fileName,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        Instant uploadedAt
) {
}
