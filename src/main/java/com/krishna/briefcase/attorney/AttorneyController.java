package com.krishna.briefcase.attorney;

import com.krishna.briefcase.attorney.dto.AttorneyRequest;
import com.krishna.briefcase.attorney.dto.AttorneyResponse;
import com.krishna.briefcase.common.security.Roles;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Managing attorney records (headcount) is an admin function ({@link Roles#ADMIN}); reads are
 * open to any authenticated user.
 */
@RestController
@RequestMapping("/api/attorneys")
public class AttorneyController {

    private final AttorneyService service;

    public AttorneyController(AttorneyService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<AttorneyResponse> create(@Valid @RequestBody AttorneyRequest request) {
        AttorneyResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/attorneys/" + created.id())).body(created);
    }

    @GetMapping
    public List<AttorneyResponse> findAll() {
        return service.findAll();
    }

    /** The attorney profile linked to the caller's own login, if any. */
    @GetMapping("/me")
    public AttorneyResponse me() {
        return service.getMyProfile();
    }

    @GetMapping("/{id}")
    public AttorneyResponse findById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public AttorneyResponse update(@PathVariable Long id, @Valid @RequestBody AttorneyRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
