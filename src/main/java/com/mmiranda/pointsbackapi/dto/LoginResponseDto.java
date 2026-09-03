package com.mmiranda.pointsbackapi.dto;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresInMinutes
) {
    public static LoginResponseDto of(String accessToken, long expiresInMinutes) {
        return new LoginResponseDto(accessToken, "Bearer", expiresInMinutes);
    }
}
