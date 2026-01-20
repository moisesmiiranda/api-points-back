package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Client;

public record ClientDto(
        String name,
        String email,
        String phone,
        String cpf,
        Integer points
) {
    public static ClientDto toDto(Client client) {
        return new ClientDto(
            client.getName(),
            client.getEmail(),
            client.getPhone(),
            client.getCpf(),
            client.getPoints()
        );
    }

    public static Client toEntity(ClientDto dto) {
        return Client.builder()
            .name(dto.name())
            .email(dto.email())
            .phone(dto.phone())
            .cpf(dto.cpf())
            .points(dto.points() != null ? dto.points() : 0)
            .build();
    }
}
