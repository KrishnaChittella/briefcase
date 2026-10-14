package com.krishna.briefcase.storage;

import com.krishna.briefcase.cases.CaseService;
import com.krishna.briefcase.cases.LegalCase;
import com.krishna.briefcase.common.exception.ResourceNotFoundException;
import com.krishna.briefcase.common.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository repository;
    private final CaseService caseService;
    private final FileStorageAdapter storageAdapter;

    public DocumentService(DocumentRepository repository, CaseService caseService,
                           FileStorageAdapter storageAdapter) {
        this.repository = repository;
        this.caseService = caseService;
        this.storageAdapter = storageAdapter;
    }

    public DocumentResponse upload(Long caseId, String originalFilename, String contentType, InputStream content) {
        LegalCase legalCase = caseService.getEntity(caseId);
        try {
            FileStorageAdapter.StoredFile stored = storageAdapter.store(caseId, originalFilename, content);
            Document document = new Document();
            document.setLegalCase(legalCase);
            document.setFileName(originalFilename);
            document.setStoragePath(stored.storagePath());
            document.setContentType(contentType);
            document.setSizeBytes(stored.sizeBytes());
            document.setUploadedBy(CurrentUser.username());
            return toResponse(repository.save(document));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> findByCase(Long caseId) {
        return repository.findByLegalCaseId(caseId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Document getEntity(Long documentId) {
        return repository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
    }

    @Transactional(readOnly = true)
    public Resource loadContent(Long documentId) {
        return storageAdapter.load(getEntity(documentId).getStoragePath());
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getLegalCase().getId(),
                document.getFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getUploadedBy(),
                document.getUploadedAt()
        );
    }
}
