package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ESTABLISHMENT_ID = "establishmentId";

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationMinutes * 60);

        var builder = Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

        if (user.getEstablishment() != null) {
            builder.claim(CLAIM_ESTABLISHMENT_ID, user.getEstablishment().getId());
        }

        return builder.signWith(signingKey).compact();
    }

    /**
     * Validates signature and expiration and decodes the claims.
     * Throws JwtException (or a subclass) for any invalid, tampered, or expired token.
     */
    public AuthenticatedUser parseToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get(CLAIM_EMAIL, String.class);
        Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
        Long establishmentId = claims.get(CLAIM_ESTABLISHMENT_ID, Long.class);

        return new AuthenticatedUser(userId, email, role, establishmentId);
    }
}
