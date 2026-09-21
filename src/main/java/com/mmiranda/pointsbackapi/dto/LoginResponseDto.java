package com.mmiranda.pointsbackapi.dto;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresInMinutes,
        boolean mustChangePassword
) {
    public static LoginResponseDto of(String accessToken, long expiresInMinutes) {
        return of(accessToken, expiresInMinutes, false);
    }

    public static LoginResponseDto of(String accessToken, long expiresInMinutes, boolean mustChangePassword) {
        return new LoginResponseDto(accessToken, "Bearer", expiresInMinutes, mustChangePassword);
    }
}
