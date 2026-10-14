package com.krishna.briefcase.common.security;

/**
 * SpEL role expressions shared across {@code @PreAuthorize} annotations. Kept as compile-time
 * constants since annotation values must be constant expressions.
 */
public final class Roles {

    private Roles() {
    }

    /** Firm leadership: full administrative access. */
    public static final String ADMIN = "hasAnyRole('SUPER_ADMIN','MANAGING_PARTNER')";

    /** Any practicing attorney (or admin) — substantive legal judgment calls. */
    public static final String ATTORNEY = "hasAnyRole('SUPER_ADMIN','MANAGING_PARTNER','SENIOR_ATTORNEY','JUNIOR_ATTORNEY')";

    /** Everyone who can perform day-to-day case/client administration, including intake staff. */
    public static final String STAFF_OR_ABOVE =
            "hasAnyRole('SUPER_ADMIN','MANAGING_PARTNER','SENIOR_ATTORNEY','JUNIOR_ATTORNEY','STAFF')";
}
