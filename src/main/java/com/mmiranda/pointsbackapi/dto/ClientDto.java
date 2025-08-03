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
        Client client = new Client();
        client.setName(dto.name());
        client.setEmail(dto.email());
        client.setPhone(dto.phone());
        client.setCpf(dto.cpf());
        client.setPoints(dto.points() != null ? dto.points() : 0);
        return client;
    }
}
