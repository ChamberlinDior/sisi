package com.pnis.backend.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) return Optional.of(ud.getUsername());
        if (principal instanceof String s && !s.equals("anonymousUser")) return Optional.of(s);
        return Optional.empty();
    }

    public static String getCurrentUsernameOrSystem() {
        return getCurrentUsername().orElse("SYSTEM");
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)
                            || a.getAuthority().equals(role));
    }
}
