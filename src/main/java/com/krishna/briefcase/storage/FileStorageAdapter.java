package com.krishna.briefcase.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * Adapter over the underlying file storage backend. Today: local disk
 * ({@link LocalFileStorageAdapter}). Swapping to S3 later means writing a new implementation
 * of this interface — callers (DocumentService) never change.
 */
public interface FileStorageAdapter {

    /**
     * Persists the given content under a storage path scoped to the case, returning the
     * path to record on the {@code Document} entity.
     */
    StoredFile store(Long caseId, String originalFilename, InputStream content) throws java.io.IOException;

    /** Loads previously stored content by its recorded storage path. */
    Resource load(String storagePath);

    /** Removes previously stored content by its recorded storage path. */
    void delete(String storagePath);

    record StoredFile(String storagePath, long sizeBytes) {
    }
}
