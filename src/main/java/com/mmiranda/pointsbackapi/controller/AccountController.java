package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.ChangePasswordRequestDto;
import com.mmiranda.pointsbackapi.dto.LoginResponseDto;
import com.mmiranda.pointsbackapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    /** Changes the caller's password and returns a fresh token (the old sessions end). */
    @PostMapping("/password")
    public LoginResponseDto changePassword(@Valid @RequestBody ChangePasswordRequestDto body, HttpServletRequest request) {
        return authService.changePassword(body, request.getRemoteAddr());
    }
}
