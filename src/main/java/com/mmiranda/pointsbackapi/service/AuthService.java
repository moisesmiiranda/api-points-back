package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.ChangePasswordRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginResponseDto;
import com.mmiranda.pointsbackapi.exception.EstablishmentSuspendedException;
import com.mmiranda.pointsbackapi.exception.InvalidCredentialsException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import com.mmiranda.pointsbackapi.security.AccessGate;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.JwtService;
import com.mmiranda.pointsbackapi.security.LoginRateLimiter;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final String GENERIC_LOGIN_ERROR = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final Clock clock;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginRateLimiter loginRateLimiter, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
        this.clock = clock;
    }

    public LoginResponseDto login(LoginRequestDto request, String clientIp) {
        loginRateLimiter.assertAllowed(clientIp, request.email());

        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null || !user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(clientIp, request.email());
            throw new InvalidCredentialsException(GENERIC_LOGIN_ERROR);
        }

        loginRateLimiter.reset(clientIp, request.email());

        // The password was right, so telling the person why they cannot enter leaks nothing to an outsider
        Establishment establishment = user.getEstablishment();
        if (establishment != null && establishment.isBlocked(LocalDate.now(clock))) {
            throw new EstablishmentSuspendedException(AccessGate.SUSPENDED_MESSAGE);
        }

        String token = jwtService.generateToken(user);
        return LoginResponseDto.of(token, jwtService.getExpirationMinutes(), user.isMustChangePassword());
    }

    /**
     * Replaces the caller's own password. Sessions that started before are ended, so a fresh token is
     * returned and the app keeps working. Wrong "current password" attempts count against the login throttle.
     */
    public LoginResponseDto changePassword(ChangePasswordRequestDto request, String clientIp) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(current.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        loginRateLimiter.assertAllowed(clientIp, user.getEmail());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(clientIp, user.getEmail());
            throw new IllegalArgumentException("The current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new IllegalArgumentException("The new password must be different from the current one");
        }
        loginRateLimiter.reset(clientIp, user.getEmail());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(LocalDateTime.now(clock));
        userRepository.save(user);

        return LoginResponseDto.of(jwtService.generateToken(user), jwtService.getExpirationMinutes(), false);
    }
}
