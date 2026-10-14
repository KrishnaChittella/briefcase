package com.krishna.briefcase.common.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small accessor for the authenticated caller's username, used for audit/ownership fields. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static String username() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
