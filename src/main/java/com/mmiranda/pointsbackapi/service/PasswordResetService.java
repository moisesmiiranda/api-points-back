package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.mail.EmailTemplates;
import com.mmiranda.pointsbackapi.model.PasswordResetToken;
import com.mmiranda.pointsbackapi.model.TokenType;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.PasswordResetTokenRepository;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.security.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Single-use, expiring links to set a password: "forgot password" and the invitation of new users.
 * Only a SHA-256 hash of each token is stored. Requesting a link never reveals whether the email is registered.
 */
@Service
public class PasswordResetService {

    static final String INVALID_LINK = "This link is invalid or has expired";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final LoginRateLimiter rateLimiter;
    private final Clock clock;
    private final String baseUrl;
    private final long resetTtlMinutes;
    private final long inviteTtlHours;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService,
                                 @Qualifier("passwordResetRateLimiter") LoginRateLimiter rateLimiter,
                                 Clock clock,
                                 @Value("${app.base-url}") String baseUrl,
                                 @Value("${app.password-reset.ttl-minutes:60}") long resetTtlMinutes,
                                 @Value("${app.password-reset.invite-ttl-hours:72}") long inviteTtlHours) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.resetTtlMinutes = resetTtlMinutes;
        this.inviteTtlHours = inviteTtlHours;
    }

    /**
     * Emails a reset link when the address belongs to an active account. Always returns normally for a known
     * or unknown address alike (the caller answers the same either way); only the rate limit can refuse.
     * Every request counts against the limit, registered or not.
     */
    @Transactional
    public void requestReset(String email, String clientIp) {
        String normalized = email.trim();
        rateLimiter.assertAllowed(clientIp, normalized);
        rateLimiter.recordFailure(clientIp, normalized);

        userRepository.findByEmail(normalized).filter(User::isActive).ifPresent(user -> {
            String token = issueToken(user, TokenType.RESET, Duration.ofMinutes(resetTtlMinutes));
            emailService.sendAsync(EmailTemplates.passwordReset(
                    user.getEmail(), user.getName(), resetLink(token), resetTtlMinutes));
        });
    }

    /** Emails a new user the link to choose their own password. */
    @Transactional
    public void sendInvite(User user) {
        String token = issueToken(user, TokenType.INVITE, Duration.ofHours(inviteTtlHours));
        emailService.sendAsync(EmailTemplates.inviteWithLink(
                user.getEmail(), user.getName(),
                user.getEstablishment() != null ? user.getEstablishment().getName() : null,
                resetLink(token), inviteTtlHours));
    }

    /** Sets the password behind a valid link, uses the link up and ends every session the user had. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken stored = tokenRepository.findByTokenHash(hash(token))
                .filter(candidate -> candidate.isUsable(now))
                .filter(candidate -> candidate.getUser().isActive())
                .orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));

        User user = stored.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        stored.setUsedAt(now);
        tokenRepository.saveAndFlush(stored);
        tokenRepository.invalidateOpenTokens(user.getId(), now);
    }

    private String issueToken(User user, TokenType type, Duration validFor) {
        if (user.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        tokenRepository.invalidateOpenTokens(user.getId(), now); // only the newest link works

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(token))
                .type(type)
                .expiresAt(now.plus(validFor))
                .build());
        return token;
    }

    private String resetLink(String token) {
        return baseUrl + "/redefinir-senha?token=" + token;
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
