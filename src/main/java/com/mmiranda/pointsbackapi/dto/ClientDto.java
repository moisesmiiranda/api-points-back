package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Client;

public record ClientDto(
        Long id,
        String name,
        String email,
        String phone,
        String cpf,
        Integer points,
        Long establishmentId
) {
    public static ClientDto toDto(Client client) {
        return new ClientDto(
            client.getId(),
            client.getName(),
            client.getEmail(),
            client.getPhone(),
            client.getCpf(),
            client.getPoints(),
            client.getEstablishment() != null ? client.getEstablishment().getId() : null
        );
    }

    /**
     * Establishment is intentionally not set here - it must be resolved and authorized
     * against the caller's establishment scope by the service layer.
     */
    public static Client toEntity(ClientDto dto) {
        return Client.builder()
            .id(dto.id())
            .name(dto.name())
            .email(dto.email())
            .phone(dto.phone())
            .cpf(dto.cpf())
            .points(dto.points() != null ? dto.points() : 0)
            .build();
    }
}
