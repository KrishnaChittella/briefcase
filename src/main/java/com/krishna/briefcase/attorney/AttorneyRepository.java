package com.krishna.briefcase.attorney;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttorneyRepository extends JpaRepository<Attorney, Long> {

    Optional<Attorney> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Attorney> findByUserId(Long userId);
}
