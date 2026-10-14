package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseRequest;
import com.krishna.briefcase.cases.dto.CaseResponse;
import com.krishna.briefcase.cases.dto.CaseStatusUpdateRequest;
import com.krishna.briefcase.common.security.Roles;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Case intake (create) is open to {@link Roles#STAFF_OR_ABOVE}; substantive edits and status
 * transitions require attorney-level judgment ({@link Roles#ATTORNEY}); deletion is an admin
 * action. Read-level ownership (attorneys see only their assigned cases) is left to the
 * {@code attorneyId} filter on search rather than enforced server-side, matching this project's
 * permissive-read / restricted-write RBAC model established in Phase 4.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService service;

    public CaseController(CaseService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.STAFF_OR_ABOVE)
    public ResponseEntity<CaseResponse> create(@Valid @RequestBody CaseRequest request) {
        CaseResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/cases/" + created.id())).body(created);
    }

    /** Paginated + filterable case search. All query params are optional. */
    @GetMapping
    public Page<CaseResponse> search(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) BillingType billingType,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long attorneyId,
            @RequestParam(required = false) Instant openedFrom,
            @RequestParam(required = false) Instant openedTo,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.search(status, billingType, clientId, attorneyId, openedFrom, openedTo, q, pageable);
    }

    @GetMapping("/{id}")
    public CaseResponse findById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ATTORNEY)
    public CaseResponse update(@PathVariable Long id, @Valid @RequestBody CaseRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Roles.ATTORNEY)
    public CaseResponse updateStatus(@PathVariable Long id, @Valid @RequestBody CaseStatusUpdateRequest request) {
        return service.transitionStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
