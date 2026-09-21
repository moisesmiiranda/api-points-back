package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.validation.Cnpj;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EstablishmentDto(
        Long id,
        @NotBlank(groups = OnCreate.class) @Size(max = 255) String name,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @NotBlank(groups = OnCreate.class) @Cnpj String cnpj,
        @NotNull(groups = OnCreate.class) @Positive Integer valuePerPoint
) {
    public static EstablishmentDto toDto(Establishment establishment) {
        return new EstablishmentDto(
            establishment.getId(),
            establishment.getName(),
            establishment.getEmail(),
            establishment.getPhone(),
            establishment.getCnpj(),
            establishment.getValuePerPoint()
        );
    }

    public static Establishment toEntity(EstablishmentDto dto) {
        return new Establishment(
            dto.id,
            dto.name,
            dto.email,
            dto.phone,
            dto.valuePerPoint,
            dto.cnpj
        );
    }
}
