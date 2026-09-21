package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.validation.Cpf;
import jakarta.validation.constraints.NotBlank;

/** Request to copy a client from another establishment of the caller's sharing group. */
public record ImportClientRequestDto(
        @NotBlank @Cpf String cpf,
        Long establishmentId
) {
}
