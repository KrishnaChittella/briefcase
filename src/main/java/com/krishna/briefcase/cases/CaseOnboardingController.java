package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseResponse;
import com.krishna.briefcase.cases.dto.OnboardCaseRequest;
import com.krishna.briefcase.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Builder-pattern shortcut: create a case and assign its initial attorney team in one call. */
@RestController
@RequestMapping("/api/cases/onboard")
public class CaseOnboardingController {

    private final CaseOnboardingService service;

    public CaseOnboardingController(CaseOnboardingService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.ATTORNEY)
    public ResponseEntity<CaseResponse> onboard(@Valid @RequestBody OnboardCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.onboard(request));
    }
}
