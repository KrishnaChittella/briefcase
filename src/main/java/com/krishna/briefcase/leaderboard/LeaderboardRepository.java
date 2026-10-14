package com.krishna.briefcase.leaderboard;

import com.krishna.briefcase.outcome.CaseResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Read-only aggregation queries backing the leaderboard. Extends the bare {@link Repository}
 * marker (not JpaRepository) since this has no business as a CRUD repository — it only reports.
 */
public interface LeaderboardRepository extends Repository<com.krishna.briefcase.attorney.Attorney, Long> {

    @Query("""
            select fd.attorney.id as attorneyId, sum(fd.amount) as total
            from FeeDistribution fd
            where fd.createdAt >= :since
            group by fd.attorney.id
            """)
    List<AttorneyAmount> totalRevenueSince(@Param("since") Instant since);

    /** LEAD/ASSOCIATE assignments on cases with an outcome recorded since the period start. */
    @Query("""
            select ca.attorney.id as attorneyId, co.result as result
            from CaseAssignment ca, CaseOutcome co
            where co.legalCase.id = ca.legalCase.id
              and ca.assignmentRole in (com.krishna.briefcase.cases.AssignmentRole.LEAD,
                                        com.krishna.briefcase.cases.AssignmentRole.ASSOCIATE)
              and co.recordedAt >= :since
            """)
    List<AttorneyResult> resultsSince(@Param("since") Instant since);

    @Query("""
            select ca.attorney.id as attorneyId, count(ca.legalCase.id) as total
            from CaseAssignment ca
            where ca.assignmentRole = com.krishna.briefcase.cases.AssignmentRole.LEAD
              and ca.legalCase.id in (select co.legalCase.id from CaseOutcome co where co.recordedAt >= :since)
            group by ca.attorney.id
            """)
    List<AttorneyCount> casesLedSince(@Param("since") Instant since);

    interface AttorneyAmount {
        Long getAttorneyId();

        BigDecimal getTotal();
    }

    interface AttorneyResult {
        Long getAttorneyId();

        CaseResult getResult();
    }

    interface AttorneyCount {
        Long getAttorneyId();

        Long getTotal();
    }
}
