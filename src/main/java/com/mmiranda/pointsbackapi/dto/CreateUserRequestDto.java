package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Role;

public record CreateUserRequestDto(
        String name,
        String email,
        String password,
        Role role,
        Long establishmentId
) {
}
