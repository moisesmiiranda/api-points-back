package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Role;

public record UpdateUserRequestDto(
        String name,
        String email,
        String password,
        Role role,
        Long establishmentId,
        Boolean active
) {
}
