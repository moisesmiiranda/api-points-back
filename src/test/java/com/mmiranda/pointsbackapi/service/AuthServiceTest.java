package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.LoginRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginResponseDto;
import com.mmiranda.pointsbackapi.exception.InvalidCredentialsException;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.dto.ChangePasswordRequestDto;
import com.mmiranda.pointsbackapi.exception.EstablishmentSuspendedException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.exception.TooManyLoginAttemptsException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentStatus;
import com.mmiranda.pointsbackapi.security.TestAuth;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import com.mmiranda.pointsbackapi.security.JwtService;
import com.mmiranda.pointsbackapi.security.LoginRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String IP = "203.0.113.7";
    private static final java.time.Clock TODAY_CLOCK =
            java.time.Clock.fixed(java.time.Instant.parse("2026-09-20T15:00:00Z"), java.time.ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    private AuthService authService;

    private User userTest;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, loginRateLimiter, TODAY_CLOCK);
        userTest = User.builder()
                .id(1L)
                .email("owner@test.com")
                .passwordHash("hashed-password")
                .role(Role.ESTABLISHMENT_OWNER)
                .active(true)
                .build();
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        LoginRequestDto request = new LoginRequestDto("owner@test.com", "correct-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(userTest)).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationMinutes()).thenReturn(60L);

        LoginResponseDto result = authService.login(request, IP);

        assertNotNull(result);
        assertEquals("signed.jwt.token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(60L, result.expiresInMinutes());
    }

    @Test
    void loginFailsForUnknownEmail() {
        LoginRequestDto request = new LoginRequestDto("nobody@test.com", "any-password");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, IP));
    }

    @Test
    void loginFailsForWrongPassword() {
        LoginRequestDto request = new LoginRequestDto("owner@test.com", "wrong-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, IP));
    }

    @Test
    void loginFailsForDeactivatedAccount() {
        userTest.setActive(false);
        LoginRequestDto request = new LoginRequestDto("owner@test.com", "correct-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, IP));
    }

    @Test
    void unknownEmailAndWrongPasswordProduceTheSameErrorMessage() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        InvalidCredentialsException unknownEmailEx = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("nobody@test.com", "x"), IP));
        InvalidCredentialsException wrongPasswordEx = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("owner@test.com", "wrong"), IP));

        assertEquals(unknownEmailEx.getMessage(), wrongPasswordEx.getMessage());
    }

    @Test
    void loginBlockedWhenRateLimitExceededDoesNotCheckCredentials() {
        LoginRequestDto request = new LoginRequestDto("owner@test.com", "any");
        doThrow(new TooManyLoginAttemptsException("blocked")).when(loginRateLimiter).assertAllowed(IP, "owner@test.com");

        assertThrows(TooManyLoginAttemptsException.class, () -> authService.login(request, IP));

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void failedLoginIsRecordedAndSuccessfulLoginResetsTheCounter() {
        LoginRequestDto wrong = new LoginRequestDto("owner@test.com", "wrong");
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);
        assertThrows(InvalidCredentialsException.class, () -> authService.login(wrong, IP));
        verify(loginRateLimiter).recordFailure(IP, "owner@test.com");
        verify(loginRateLimiter, never()).reset(any(), any());

        LoginRequestDto right = new LoginRequestDto("owner@test.com", "correct-password");
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(userTest)).thenReturn("signed.jwt.token");
        authService.login(right, IP);
        verify(loginRateLimiter).reset(IP, "owner@test.com");
    }

    // ------------------------------------------------------------------ establishment status and forced change

    private void stubValidLogin(User user) {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
    }

    private User userOf(EstablishmentStatus status, LocalDate trialEndsAt) {
        Establishment establishment = new Establishment();
        establishment.setId(3L);
        establishment.setStatus(status);
        establishment.setTrialEndsAt(trialEndsAt);
        userTest.setEstablishment(establishment);
        return userTest;
    }

    @Test
    void aSuspendedEstablishmentCannotLogInEvenWithTheRightPassword() {
        stubValidLogin(userOf(EstablishmentStatus.SUSPENDED, null));

        assertThrows(EstablishmentSuspendedException.class,
                () -> authService.login(new LoginRequestDto("owner@test.com", "correct-password"), IP));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void anEndedTrialCannotLogIn() {
        stubValidLogin(userOf(EstablishmentStatus.TRIAL, LocalDate.of(2026, 9, 19)));

        assertThrows(EstablishmentSuspendedException.class,
                () -> authService.login(new LoginRequestDto("owner@test.com", "correct-password"), IP));
    }

    @Test
    void aWrongPasswordStillGetsTheGenericErrorEvenForASuspendedEstablishment() {
        userOf(EstablishmentStatus.SUSPENDED, null);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("owner@test.com", "wrong"), IP));
    }

    @Test
    void aRunningTrialOrActiveEstablishmentLogsIn() {
        stubValidLogin(userOf(EstablishmentStatus.TRIAL, LocalDate.of(2026, 9, 20)));
        when(jwtService.generateToken(userTest)).thenReturn("token");
        when(jwtService.getExpirationMinutes()).thenReturn(60L);

        assertEquals("token", authService.login(new LoginRequestDto("owner@test.com", "correct-password"), IP).accessToken());

        userOf(EstablishmentStatus.ACTIVE, null);
        assertEquals("token", authService.login(new LoginRequestDto("owner@test.com", "correct-password"), IP).accessToken());
    }

    @Test
    void theLoginResponseTellsWhetherThePasswordMustBeChanged() {
        userTest.setMustChangePassword(true);
        stubValidLogin(userTest);
        when(jwtService.generateToken(userTest)).thenReturn("token");
        when(jwtService.getExpirationMinutes()).thenReturn(60L);

        assertEquals(true, authService.login(new LoginRequestDto("owner@test.com", "correct-password"), IP).mustChangePassword());
    }

    // ------------------------------------------------------------------ changing one's own password

    private void asUser() {
        TestAuth.asEstablishmentOwner(1L); // user id 2
        userTest.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(userTest));
    }

    @AfterEach
    void clearAuth() {
        TestAuth.clear();
    }

    @Test
    void changePasswordSetsTheNewHashEndsOldSessionsAndReturnsAFreshToken() {
        asUser();
        userTest.setMustChangePassword(true);
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("NovaSenha123")).thenReturn("new-hash");
        when(jwtService.generateToken(userTest)).thenReturn("fresh-token");
        when(jwtService.getExpirationMinutes()).thenReturn(60L);

        LoginResponseDto result = authService.changePassword(new ChangePasswordRequestDto("correct-password", "NovaSenha123"), IP);

        assertEquals("fresh-token", result.accessToken());
        assertEquals(false, result.mustChangePassword());
        assertEquals("new-hash", userTest.getPasswordHash());
        assertEquals(false, userTest.isMustChangePassword());
        assertNotNull(userTest.getPasswordChangedAt());
        verify(userRepository).save(userTest);
        verify(loginRateLimiter).reset(IP, "owner@test.com");
    }

    @Test
    void aWrongCurrentPasswordIsRejectedAndCountsAgainstTheThrottle() {
        asUser();
        when(passwordEncoder.matches("nope", "hashed-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword(new ChangePasswordRequestDto("nope", "NovaSenha123"), IP));

        verify(loginRateLimiter).recordFailure(IP, "owner@test.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void theNewPasswordMustDifferFromTheCurrentOne() {
        asUser();
        when(passwordEncoder.matches("SenhaAtual123", "hashed-password")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword(new ChangePasswordRequestDto("SenhaAtual123", "SenhaAtual123"), IP));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordIsThrottledAndNeedsAnExistingUser() {
        asUser();
        doThrow(new TooManyLoginAttemptsException("slow down")).when(loginRateLimiter).assertAllowed(IP, "owner@test.com");
        assertThrows(TooManyLoginAttemptsException.class,
                () -> authService.changePassword(new ChangePasswordRequestDto("x", "NovaSenha123"), IP));

        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> authService.changePassword(new ChangePasswordRequestDto("x", "NovaSenha123"), IP));
    }
}
