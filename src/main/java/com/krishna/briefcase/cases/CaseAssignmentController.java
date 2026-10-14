package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseAssignmentRequest;
import com.krishna.briefcase.cases.dto.CaseAssignmentResponse;
import com.krishna.briefcase.common.security.Roles;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Assigning attorneys to a case is an attorney-level judgment call ({@link Roles#ATTORNEY}). */
@RestController
@RequestMapping("/api/cases/{caseId}/assignments")
public class CaseAssignmentController {

    private final CaseAssignmentService service;

    public CaseAssignmentController(CaseAssignmentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.ATTORNEY)
    public ResponseEntity<CaseAssignmentResponse> assign(@PathVariable Long caseId,
                                                          @Valid @RequestBody CaseAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.assign(caseId, request));
    }

    @GetMapping
    public List<CaseAssignmentResponse> list(@PathVariable Long caseId) {
        return service.findByCase(caseId);
    }

    @DeleteMapping("/{attorneyId}")
    @PreAuthorize(Roles.ATTORNEY)
    public ResponseEntity<Void> unassign(@PathVariable Long caseId, @PathVariable Long attorneyId) {
        service.unassign(caseId, attorneyId);
        return ResponseEntity.noContent().build();
    }
}
