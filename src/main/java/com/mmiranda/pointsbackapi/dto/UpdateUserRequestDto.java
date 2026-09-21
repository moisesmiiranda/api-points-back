package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto(
        @Size(min = 1, max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(min = 8, max = 72) String password,
        Role role,
        Long establishmentId,
        Boolean active
) {
}
