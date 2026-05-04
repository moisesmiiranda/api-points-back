package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Establishment;

public record EstablishmentDto(
        Long id,
        String name,
        String email,
        String phone,
        String cnpj,
        Integer valuePerPoint
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
