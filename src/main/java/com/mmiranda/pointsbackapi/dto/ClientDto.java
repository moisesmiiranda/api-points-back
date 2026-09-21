package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.validation.Cpf;
import com.mmiranda.pointsbackapi.validation.Documents;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ClientDto(
        Long id,
        @NotBlank(groups = OnCreate.class) @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @NotBlank(groups = OnCreate.class) @Cpf String cpf,
        @PositiveOrZero Integer points,
        Long establishmentId
) {
    public static ClientDto toDto(Client client) {
        return new ClientDto(
            client.getId(),
            client.getName(),
            client.getEmail(),
            client.getPhone(),
            Documents.formatCpf(client.getCpf()),
            client.getPoints(),
            client.getEstablishment() != null ? client.getEstablishment().getId() : null
        );
    }
}
