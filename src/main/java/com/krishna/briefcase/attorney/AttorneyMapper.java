package com.krishna.briefcase.attorney;

import com.krishna.briefcase.attorney.dto.AttorneyRequest;
import com.krishna.briefcase.attorney.dto.AttorneyResponse;
import org.springframework.stereotype.Component;

@Component
public class AttorneyMapper {

    public Attorney toEntity(AttorneyRequest request) {
        Attorney attorney = new Attorney();
        attorney.setFirstName(request.firstName());
        attorney.setLastName(request.lastName());
        attorney.setEmail(request.email());
        attorney.setActive(request.active() == null || request.active());
        return attorney;
    }

    public void update(Attorney attorney, AttorneyRequest request) {
        attorney.setFirstName(request.firstName());
        attorney.setLastName(request.lastName());
        attorney.setEmail(request.email());
        if (request.active() != null) {
            attorney.setActive(request.active());
        }
    }

    public AttorneyResponse toResponse(Attorney attorney) {
        return new AttorneyResponse(
                attorney.getId(),
                attorney.getFirstName(),
                attorney.getLastName(),
                attorney.getEmail(),
                attorney.isActive(),
                attorney.getUser() != null ? attorney.getUser().getId() : null,
                attorney.getCreatedAt()
        );
    }
}
