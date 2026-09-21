package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.ForgotPasswordRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginResponseDto;
import com.mmiranda.pointsbackapi.dto.MessageDto;
import com.mmiranda.pointsbackapi.dto.ResetPasswordRequestDto;
import com.mmiranda.pointsbackapi.service.AuthService;
import com.mmiranda.pointsbackapi.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto loginRequestDto, HttpServletRequest request) {
        return authService.login(loginRequestDto, request.getRemoteAddr());
    }

    /** Always the same answer, whether or not the email is registered. */
    @PostMapping("/forgot-password")
    public MessageDto forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto body, HttpServletRequest request) {
        passwordResetService.requestReset(body.email(), request.getRemoteAddr());
        return new MessageDto("If the email is registered, we sent instructions to reset the password");
    }

    @PostMapping("/reset-password")
    public MessageDto resetPassword(@Valid @RequestBody ResetPasswordRequestDto body) {
        passwordResetService.resetPassword(body.token(), body.newPassword());
        return new MessageDto("Password updated");
    }
}
