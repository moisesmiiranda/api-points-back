package com.mmiranda.pointsbackapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmiranda.pointsbackapi.exception.ApiError;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                    ObjectMapper objectMapper, Clock clock) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parseClaims(token);
                // The token only proves who the caller is. Whether the account is still active, and its
                // current role and establishment, come from the database, so deactivating a user or changing
                // their role takes effect on the next request instead of at token expiry.
                User account = userRepository.findById(Long.valueOf(claims.getSubject())).orElse(null);
                if (account == null || !account.isActive() || issuedBeforePasswordChange(claims, account)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                Optional<AccessGate.Block> block = AccessGate.check(account, request.getMethod(),
                        request.getRequestURI(), LocalDate.now(clock));
                if (block.isPresent()) {
                    SecurityContextHolder.clearContext();
                    writeBlock(response, block.get());
                    return;
                }

                AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                        account.getId(),
                        account.getEmail(),
                        account.getRole(),
                        account.getEstablishment() != null ? account.getEstablishment().getId() : null);
                List<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name())
                );
                var authentication = new UsernamePasswordAuthenticationToken(
                        authenticatedUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired/tampered token: leave the context unauthenticated so the
                // configured AuthenticationEntryPoint returns a 401 for protected endpoints.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /** A password change ends the sessions that started before it (compared at whole-second precision, like iat). */
    private boolean issuedBeforePasswordChange(Claims claims, User account) {
        if (account.getPasswordChangedAt() == null || claims.getIssuedAt() == null) {
            return false;
        }
        Instant changedAt = account.getPasswordChangedAt().atZone(clock.getZone()).toInstant().truncatedTo(ChronoUnit.SECONDS);
        return claims.getIssuedAt().toInstant().isBefore(changedAt);
    }

    private void writeBlock(HttpServletResponse response, AccessGate.Block block) throws IOException {
        response.setStatus(block.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiError.of(block.status().value(), block.error(), block.message()));
    }
}
