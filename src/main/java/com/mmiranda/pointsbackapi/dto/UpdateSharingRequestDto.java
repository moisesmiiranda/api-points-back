package com.mmiranda.pointsbackapi.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSharingRequestDto(@NotNull Boolean shareClients) {
}
