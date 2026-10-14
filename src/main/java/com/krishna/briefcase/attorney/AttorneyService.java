package com.krishna.briefcase.attorney;

import com.krishna.briefcase.attorney.dto.AttorneyRequest;
import com.krishna.briefcase.attorney.dto.AttorneyResponse;
import com.krishna.briefcase.auth.AppUserPrincipal;
import com.krishna.briefcase.auth.User;
import com.krishna.briefcase.auth.UserRepository;
import com.krishna.briefcase.common.exception.DuplicateResourceException;
import com.krishna.briefcase.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AttorneyService {

    private final AttorneyRepository repository;
    private final AttorneyMapper mapper;
    private final UserRepository userRepository;

    public AttorneyService(AttorneyRepository repository, AttorneyMapper mapper, UserRepository userRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    public AttorneyResponse create(AttorneyRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Attorney already exists with email " + request.email());
        }
        Attorney attorney = mapper.toEntity(request);
        linkUserIfRequested(attorney, request.userId());
        Attorney saved = repository.save(attorney);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttorneyResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttorneyResponse getById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Attorney getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Attorney", id));
    }

    /** Resolves the attorney profile linked to the currently authenticated user. */
    @Transactional(readOnly = true)
    public AttorneyResponse getMyProfile() {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Attorney attorney = repository.findByUserId(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No attorney profile linked to user " + principal.getUsername()));
        return mapper.toResponse(attorney);
    }

    public AttorneyResponse update(Long id, AttorneyRequest request) {
        Attorney attorney = getEntity(id);
        mapper.update(attorney, request);
        linkUserIfRequested(attorney, request.userId());
        return mapper.toResponse(attorney);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Attorney", id);
        }
        repository.deleteById(id);
    }

    private void linkUserIfRequested(Attorney attorney, Long userId) {
        if (userId == null) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        attorney.setUser(user);
    }
}
