package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.LoginRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginResponseDto;
import com.mmiranda.pointsbackapi.exception.InvalidCredentialsException;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User userTest;

    @BeforeEach
    void setUp() {
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

        LoginResponseDto result = authService.login(request);

        assertNotNull(result);
        assertEquals("signed.jwt.token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(60L, result.expiresInMinutes());
    }

    @Test
    void loginFailsForUnknownEmail() {
        LoginRequestDto request = new LoginRequestDto("nobody@test.com", "any-password");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginFailsForWrongPassword() {
        LoginRequestDto request = new LoginRequestDto("owner@test.com", "wrong-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginFailsForDeactivatedAccount() {
        userTest.setActive(false);
        LoginRequestDto request = new LoginRequestDto("owner@test.com", "correct-password");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void unknownEmailAndWrongPasswordProduceTheSameErrorMessage() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(userTest));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        InvalidCredentialsException unknownEmailEx = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("nobody@test.com", "x")));
        InvalidCredentialsException wrongPasswordEx = assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("owner@test.com", "wrong")));

        assertEquals(unknownEmailEx.getMessage(), wrongPasswordEx.getMessage());
    }
}
