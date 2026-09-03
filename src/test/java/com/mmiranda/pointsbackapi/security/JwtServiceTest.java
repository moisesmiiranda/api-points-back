package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void generatesAndParsesTokenForEstablishmentUser() {
        Establishment establishment = new Establishment();
        establishment.setId(5L);

        User user = User.builder()
                .id(1L)
                .email("owner@test.com")
                .role(Role.ESTABLISHMENT_OWNER)
                .establishment(establishment)
                .build();

        String token = jwtService.generateToken(user);
        assertNotNull(token);

        AuthenticatedUser parsed = jwtService.parseToken(token);

        assertEquals(1L, parsed.userId());
        assertEquals("owner@test.com", parsed.email());
        assertEquals(Role.ESTABLISHMENT_OWNER, parsed.role());
        assertEquals(5L, parsed.establishmentId());
    }

    @Test
    void generatesTokenWithoutEstablishmentForPlatformAdmin() {
        User user = User.builder()
                .id(2L)
                .email("admin@test.com")
                .role(Role.PLATFORM_ADMIN)
                .establishment(null)
                .build();

        String token = jwtService.generateToken(user);
        AuthenticatedUser parsed = jwtService.parseToken(token);

        assertEquals(Role.PLATFORM_ADMIN, parsed.role());
        assertNull(parsed.establishmentId());
    }

    @Test
    void rejectsTamperedToken() {
        User user = User.builder().id(1L).email("a@test.com").role(Role.PLATFORM_ADMIN).build();
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(SignatureException.class, () -> jwtService.parseToken(tampered));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService otherService = new JwtService("a-completely-different-secret-key-32-bytes!", 60);
        User user = User.builder().id(1L).email("a@test.com").role(Role.PLATFORM_ADMIN).build();
        String token = otherService.generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.parseToken(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService alreadyExpiredService = new JwtService(SECRET, -1);
        User user = User.builder().id(1L).email("a@test.com").role(Role.PLATFORM_ADMIN).build();
        String token = alreadyExpiredService.generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> alreadyExpiredService.parseToken(token));
    }

    @Test
    void exposesConfiguredExpiration() {
        assertEquals(60, jwtService.getExpirationMinutes());
    }
}
