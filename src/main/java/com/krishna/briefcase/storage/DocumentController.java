package com.krishna.briefcase.storage;

import com.krishna.briefcase.common.security.Roles;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cases/{caseId}/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.STAFF_OR_ABOVE)
    public ResponseEntity<DocumentResponse> upload(@PathVariable Long caseId,
                                                    @RequestParam("file") MultipartFile file) {
        try {
            DocumentResponse response = service.upload(caseId, file.getOriginalFilename(),
                    file.getContentType(), file.getInputStream());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    @GetMapping
    public List<DocumentResponse> list(@PathVariable Long caseId) {
        return service.findByCase(caseId);
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long caseId, @PathVariable Long documentId) {
        Document metadata = service.getEntity(documentId);
        Resource resource = service.loadContent(documentId);
        MediaType mediaType = metadata.getContentType() != null
                ? MediaType.parseMediaType(metadata.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
    }
}
