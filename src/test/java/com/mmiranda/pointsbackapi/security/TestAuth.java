package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Test helper to populate the SecurityContext with an AuthenticatedUser, mirroring what
 * JwtAuthenticationFilter does for a real request. Services read the caller via
 * SecurityUtils.getCurrentUser(), so unit tests that exercise a real (non-mocked) service
 * must set this up in @BeforeEach and clear it in @AfterEach.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static void asPlatformAdmin() {
        set(new AuthenticatedUser(1L, "admin@test.com", Role.PLATFORM_ADMIN, null));
    }

    public static void asEstablishmentOwner(Long establishmentId) {
        set(new AuthenticatedUser(2L, "owner@test.com", Role.ESTABLISHMENT_OWNER, establishmentId));
    }

    public static void asEstablishmentStaff(Long establishmentId) {
        set(new AuthenticatedUser(3L, "staff@test.com", Role.ESTABLISHMENT_STAFF, establishmentId));
    }

    public static void set(AuthenticatedUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
