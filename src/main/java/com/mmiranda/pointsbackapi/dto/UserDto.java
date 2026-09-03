package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;

public record UserDto(
        Long id,
        String name,
        String email,
        Role role,
        Long establishmentId,
        boolean active
) {
    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getEstablishment() != null ? user.getEstablishment().getId() : null,
                user.isActive()
        );
    }
}
