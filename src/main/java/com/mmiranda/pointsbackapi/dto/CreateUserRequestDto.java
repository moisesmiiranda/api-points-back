package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDto(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        /** Optional: when omitted the person is invited by email to choose their own password. */
        @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        Long establishmentId
) {
}
