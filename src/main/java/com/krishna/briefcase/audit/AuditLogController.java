package com.krishna.briefcase.audit;

import com.krishna.briefcase.audit.dto.AuditLogResponse;
import com.krishna.briefcase.common.security.Roles;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only: view a case's change history. */
@RestController
@RequestMapping("/api/cases/{caseId}/audit-log")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.ADMIN)
    public List<AuditLogResponse> list(@PathVariable Long caseId) {
        return service.findForCase(caseId);
    }
}
