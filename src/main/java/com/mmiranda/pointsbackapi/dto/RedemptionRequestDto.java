package com.mmiranda.pointsbackapi.dto;

import jakarta.validation.constraints.NotNull;

public record RedemptionRequestDto(@NotNull Long clientId, @NotNull Long rewardId) {
}
