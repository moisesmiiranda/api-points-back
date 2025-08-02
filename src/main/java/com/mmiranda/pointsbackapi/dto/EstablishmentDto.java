package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Establishment;

public record EstablishmentDto(
        String name,
        String email,
        String phone,
        String cnpj
) {
    public static EstablishmentDto toDto(Establishment establishment) {
        return new EstablishmentDto(
            establishment.getName(),
            establishment.getEmail(),
            establishment.getPhone(),
            establishment.getCnpj()
        );
    }

    public static Establishment toEntity(EstablishmentDto dto) {
        Establishment establishment = new Establishment();
        establishment.setName(dto.name());
        establishment.setEmail(dto.email());
        establishment.setPhone(dto.phone());
        establishment.setCnpj(dto.cnpj());
        return establishment;
    }
}
