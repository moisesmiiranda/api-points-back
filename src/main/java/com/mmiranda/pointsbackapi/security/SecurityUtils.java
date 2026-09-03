package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No authenticated user in the security context");
        }
        return user;
    }

    /**
     * PLATFORM_ADMIN may act on any establishment. Every other role may only act on its
     * own establishment - any other id (existent or not) is rejected identically, so a
     * 403 never reveals whether the target establishment exists.
     */
    public static void requireEstablishmentAccess(Long establishmentId) {
        AuthenticatedUser current = getCurrentUser();
        if (current.isPlatformAdmin()) {
            return;
        }
        if (!current.belongsToEstablishment(establishmentId)) {
            throw new ForbiddenException("You do not have access to this establishment's data");
        }
    }
}
