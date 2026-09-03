package com.mmiranda.pointsbackapi.dto;

public record LoginRequestDto(
        String email,
        String password
) {
}
