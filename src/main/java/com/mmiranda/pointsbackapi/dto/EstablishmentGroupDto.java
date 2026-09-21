package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.EstablishmentGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstablishmentGroupDto(
        Long id,
        @NotBlank @Size(max = 255) String name
) {
    public static EstablishmentGroupDto toDto(EstablishmentGroup group) {
        return new EstablishmentGroupDto(group.getId(), group.getName());
    }
}
