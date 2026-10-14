package com.krishna.briefcase.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Local-disk implementation of {@link FileStorageAdapter}, rooted at {@code storage.root-dir}. */
@Component
@EnableConfigurationProperties(StorageProperties.class)
public class LocalFileStorageAdapter implements FileStorageAdapter {

    private final Path root;

    public LocalFileStorageAdapter(StorageProperties properties) throws IOException {
        this.root = Path.of(properties.rootDir()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public StoredFile store(Long caseId, String originalFilename, InputStream content) throws IOException {
        String safeName = StringUtils.cleanPath(originalFilename);
        String storedName = UUID.randomUUID() + "_" + safeName;
        Path caseDir = root.resolve(caseId.toString()).normalize();
        Files.createDirectories(caseDir);
        Path target = caseDir.resolve(storedName).normalize();
        if (!target.startsWith(caseDir)) {
            throw new IOException("Invalid file name: " + originalFilename);
        }
        long size = Files.copy(content, target);
        String relativePath = caseId + "/" + storedName;
        return new StoredFile(relativePath, size);
    }

    @Override
    public Resource load(String storagePath) {
        return new FileSystemResource(root.resolve(storagePath).normalize());
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(root.resolve(storagePath).normalize());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete stored file", e);
        }
    }
}
