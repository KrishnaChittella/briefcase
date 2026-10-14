package com.krishna.briefcase.cases;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

/** Composable filter predicates for the case list/search endpoint. */
public final class CaseSpecifications {

    private CaseSpecifications() {
    }

    public static Specification<LegalCase> hasStatus(CaseStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<LegalCase> hasBillingType(BillingType billingType) {
        return (root, query, cb) -> billingType == null ? null : cb.equal(root.get("billingType"), billingType);
    }

    public static Specification<LegalCase> hasClient(Long clientId) {
        return (root, query, cb) -> clientId == null ? null : cb.equal(root.get("client").get("id"), clientId);
    }

    /** Cases with any assignment to the given attorney, via a subquery (no join fan-out). */
    public static Specification<LegalCase> assignedToAttorney(Long attorneyId) {
        return (root, query, cb) -> {
            if (attorneyId == null) {
                return null;
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<CaseAssignment> assignmentRoot = subquery.from(CaseAssignment.class);
            subquery.select(assignmentRoot.get("legalCase").get("id"))
                    .where(cb.equal(assignmentRoot.get("attorney").get("id"), attorneyId));
            return root.get("id").in(subquery);
        };
    }

    public static Specification<LegalCase> openedAfter(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("openedAt"), from);
    }

    public static Specification<LegalCase> openedBefore(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("openedAt"), to);
    }

    /** Matches case number or title, case-insensitive substring. */
    public static Specification<LegalCase> textSearch(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("caseNumber")), like),
                    cb.like(cb.lower(root.get("title")), like)
            );
        };
    }
}
