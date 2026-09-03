package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Role;

/**
 * The authenticated caller's identity, decoded from the JWT claims on every request.
 * establishmentId is null only for PLATFORM_ADMIN.
 */
public record AuthenticatedUser(
        Long userId,
        String email,
        Role role,
        Long establishmentId
) {
    public boolean isPlatformAdmin() {
        return role == Role.PLATFORM_ADMIN;
    }

    public boolean belongsToEstablishment(Long otherEstablishmentId) {
        return establishmentId != null && establishmentId.equals(otherEstablishmentId);
    }
}
