package com.krishna.briefcase.leaderboard;

import com.krishna.briefcase.attorney.Attorney;
import com.krishna.briefcase.attorney.AttorneyRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Batch-resolves attorney display names for ranking strategies, avoiding N+1 lookups. */
@Component
class AttorneyNameLookup {

    private final AttorneyRepository attorneyRepository;

    AttorneyNameLookup(AttorneyRepository attorneyRepository) {
        this.attorneyRepository = attorneyRepository;
    }

    Map<Long, String> namesFor(Collection<Long> attorneyIds) {
        return attorneyRepository.findAllById(attorneyIds).stream()
                .collect(Collectors.toMap(Attorney::getId, a -> a.getFirstName() + " " + a.getLastName()));
    }
}
