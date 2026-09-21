package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.exception.TooManyLoginAttemptsException;
import com.mmiranda.pointsbackapi.mail.EmailMessage;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.PasswordResetToken;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.TokenType;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.PasswordResetTokenRepository;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-20T15:00:00Z");
    private static final LocalDateTime NOW_LOCAL = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private LoginRateLimiter rateLimiter;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, emailService, rateLimiter,
                Clock.fixed(NOW, ZoneOffset.UTC), "https://app.loja.com.br/", 60, 72);
        Establishment establishment = new Establishment();
        establishment.setName("Padaria Boa");
        user = User.builder().id(5L).name("Ana").email("ana@loja.com").passwordHash("old").role(Role.ESTABLISHMENT_OWNER)
                .establishment(establishment).active(true).build();
    }

    private String tokenFromSentEmail() {
        ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).sendAsync(email.capture());
        Matcher matcher = TOKEN_IN_LINK.matcher(email.getValue().text());
        assertTrue(matcher.find(), "the email must carry the link");
        assertTrue(email.getValue().text().contains("https://app.loja.com.br/redefinir-senha?token="));
        return matcher.group(1);
    }

    // ------------------------------------------------------------------ requesting a reset

    @Test
    void aKnownActiveUserGetsALinkAndOnlyTheHashOfTheTokenIsStored() {
        when(userRepository.findByEmail("ana@loja.com")).thenReturn(Optional.of(user));

        service.requestReset("  ana@loja.com ", "10.0.0.1");

        String token = tokenFromSentEmail();
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        assertEquals(TokenType.RESET, saved.getValue().getType());
        assertEquals(PasswordResetService.hash(token), saved.getValue().getTokenHash());
        assertNotEquals(token, saved.getValue().getTokenHash());
        assertEquals(64, saved.getValue().getTokenHash().length());
        assertEquals(NOW_LOCAL.plusMinutes(60), saved.getValue().getExpiresAt());
        assertTrue(token.length() >= 43, "256 bits of randomness");
        verify(tokenRepository).invalidateOpenTokens(5L, NOW_LOCAL); // only the newest link works
    }

    @Test
    void anUnknownEmailGetsNoLinkAndNoErrorSoNothingLeaks() {
        when(userRepository.findByEmail("ninguem@loja.com")).thenReturn(Optional.empty());

        service.requestReset("ninguem@loja.com", "10.0.0.1");

        verify(emailService, never()).sendAsync(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void aDeactivatedUserGetsNoLink() {
        user.setActive(false);
        when(userRepository.findByEmail("ana@loja.com")).thenReturn(Optional.of(user));

        service.requestReset("ana@loja.com", "10.0.0.1");

        verify(emailService, never()).sendAsync(any());
    }

    @Test
    void everyRequestCountsAgainstTheLimitWhetherOrNotTheEmailExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        service.requestReset("ninguem@loja.com", "10.0.0.1");

        verify(rateLimiter).assertAllowed("10.0.0.1", "ninguem@loja.com");
        verify(rateLimiter).recordFailure("10.0.0.1", "ninguem@loja.com");
    }

    @Test
    void whenThrottledNothingIsLookedUpOrSent() {
        doThrow(new TooManyLoginAttemptsException("slow down")).when(rateLimiter).assertAllowed("10.0.0.1", "ana@loja.com");

        assertThrows(TooManyLoginAttemptsException.class, () -> service.requestReset("ana@loja.com", "10.0.0.1"));

        verify(userRepository, never()).findByEmail(any());
        verify(emailService, never()).sendAsync(any());
    }

    // ------------------------------------------------------------------ invitations

    @Test
    void anInviteIsAnInviteTokenThatLastsLongerAndMentionsTheEstablishment() {
        service.sendInvite(user);

        String token = tokenFromSentEmail();
        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        assertEquals(TokenType.INVITE, saved.getValue().getType());
        assertEquals(NOW_LOCAL.plusHours(72), saved.getValue().getExpiresAt());
        assertEquals(PasswordResetService.hash(token), saved.getValue().getTokenHash());
        ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailService).sendAsync(email.capture());
        assertTrue(email.getValue().text().contains("Padaria Boa"));
    }

    @Test
    void inviteWorksForAnAdminWithoutAnEstablishment() {
        user.setEstablishment(null);

        service.sendInvite(user);

        verify(emailService).sendAsync(any(EmailMessage.class));
    }

    @Test
    void invitingAnUnsavedUserIsRejected() {
        user.setId(null);

        assertThrows(ResourceNotFoundException.class, () -> service.sendInvite(user));
    }

    // ------------------------------------------------------------------ using a link

    private PasswordResetToken storedToken(String raw, LocalDateTime expiresAt, LocalDateTime usedAt) {
        PasswordResetToken token = PasswordResetToken.builder().user(user).tokenHash(PasswordResetService.hash(raw))
                .type(TokenType.RESET).expiresAt(expiresAt).usedAt(usedAt).build();
        when(tokenRepository.findByTokenHash(PasswordResetService.hash(raw))).thenReturn(Optional.of(token));
        return token;
    }

    @Test
    void aValidLinkSetsThePasswordUsesTheLinkUpAndEndsOldSessions() {
        PasswordResetToken token = storedToken("good", NOW_LOCAL.plusMinutes(10), null);
        user.setMustChangePassword(true);
        when(passwordEncoder.encode("NovaSenha123")).thenReturn("new-hash");

        service.resetPassword("good", "NovaSenha123");

        assertEquals("new-hash", user.getPasswordHash());
        assertFalse(user.isMustChangePassword());
        assertEquals(NOW_LOCAL, user.getPasswordChangedAt());
        assertEquals(NOW_LOCAL, token.getUsedAt());
        verify(userRepository).save(user);
        verify(tokenRepository).saveAndFlush(token);
        verify(tokenRepository).invalidateOpenTokens(5L, NOW_LOCAL);
    }

    @Test
    void anUnknownExpiredOrUsedLinkAllGiveTheSameError() {
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("nope"))).thenReturn(Optional.empty());
        storedToken("old", NOW_LOCAL.minusSeconds(1), null);
        storedToken("used", NOW_LOCAL.plusMinutes(10), NOW_LOCAL.minusMinutes(1));

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> service.resetPassword("nope", "NovaSenha123"));
        IllegalArgumentException expired = assertThrows(IllegalArgumentException.class, () -> service.resetPassword("old", "NovaSenha123"));
        IllegalArgumentException used = assertThrows(IllegalArgumentException.class, () -> service.resetPassword("used", "NovaSenha123"));

        assertEquals(unknown.getMessage(), expired.getMessage());
        assertEquals(unknown.getMessage(), used.getMessage());
        verify(userRepository, never()).save(any());
        assertEquals("old", user.getPasswordHash());
        assertNull(user.getPasswordChangedAt());
    }

    @Test
    void aLinkOfADeactivatedUserIsRejected() {
        storedToken("good", NOW_LOCAL.plusMinutes(10), null);
        user.setActive(false);

        assertThrows(IllegalArgumentException.class, () -> service.resetPassword("good", "NovaSenha123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void theTokenHashIsAPlainSha256Hex() {
        assertEquals("2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae", PasswordResetService.hash("foo"));
        assertNotNull(PasswordResetService.hash("x"));
    }
}
