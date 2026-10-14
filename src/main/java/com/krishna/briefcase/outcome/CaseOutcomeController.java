package com.krishna.briefcase.outcome;

import com.krishna.briefcase.common.security.Roles;
import com.krishna.briefcase.outcome.dto.CaseOutcomeResponse;
import com.krishna.briefcase.outcome.dto.RecordOutcomeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases/{caseId}/outcome")
public class CaseOutcomeController {

    private final CaseOutcomeService service;

    public CaseOutcomeController(CaseOutcomeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.ATTORNEY)
    public ResponseEntity<CaseOutcomeResponse> record(@PathVariable Long caseId,
                                                       @Valid @RequestBody RecordOutcomeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.recordOutcome(caseId, request));
    }

    @GetMapping
    public CaseOutcomeResponse get(@PathVariable Long caseId) {
        return service.getByCase(caseId);
    }
}
