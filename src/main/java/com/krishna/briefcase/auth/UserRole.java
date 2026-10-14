package com.krishna.briefcase.auth;

/**
 * Security role driving RBAC. Distinct from the per-case AssignmentRole (LEAD/ASSOCIATE/PARALEGAL).
 * Seniority of an attorney is derived from the linked user's role.
 */
public enum UserRole {
    SUPER_ADMIN,
    MANAGING_PARTNER,
    SENIOR_ATTORNEY,
    JUNIOR_ATTORNEY,
    PARALEGAL,
    STAFF;

    /** Spring Security authority string (hasRole("X") checks for "ROLE_X"). */
    public String authority() {
        return "ROLE_" + name();
    }
}
