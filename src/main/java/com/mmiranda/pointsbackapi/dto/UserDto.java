package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;

public record UserDto(
        Long id,
        String name,
        String email,
        Role role,
        Long establishmentId,
        boolean active,
        boolean mustChangePassword
) {
    public UserDto(Long id, String name, String email, Role role, Long establishmentId, boolean active) {
        this(id, name, email, role, establishmentId, active, false);
    }

    public static UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getEstablishment() != null ? user.getEstablishment().getId() : null,
                user.isActive(),
                user.isMustChangePassword()
        );
    }
}
