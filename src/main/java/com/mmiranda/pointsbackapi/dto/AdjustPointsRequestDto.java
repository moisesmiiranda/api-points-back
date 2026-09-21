package com.mmiranda.pointsbackapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Manual correction of a balance: {@code points} is signed and never zero, {@code reason} is mandatory. */
public record AdjustPointsRequestDto(
        @NotNull Integer points,
        @NotBlank @Size(max = 255) String reason
) {
}
