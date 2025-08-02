package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Client;

public record ClientDto(
        String name,
        String email,
        String phone,
        String cpf
) {
    public static ClientDto toDto(Client client) {
        return new ClientDto(
            client.getName(),
            client.getEmail(),
            client.getPhone(),
            client.getCpf()
        );
    }

    public static Client toEntity(ClientDto dto) {
        Client client = new Client();
        client.setName(dto.name());
        client.setEmail(dto.email());
        client.setPhone(dto.phone());
        client.setCpf(dto.cpf());
        return client;
    }
}
