package com.mmiranda.pointsbackapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
