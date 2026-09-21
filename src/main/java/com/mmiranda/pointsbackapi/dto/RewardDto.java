package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Reward;
import com.mmiranda.pointsbackapi.model.RewardType;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RewardDto(
        Long id,
        Long establishmentId,
        @NotBlank(groups = OnCreate.class) @Size(max = 255) String name,
        @Size(max = 500) String description,
        @NotNull(groups = OnCreate.class) RewardType type,
        @NotNull(groups = OnCreate.class) @Positive Integer pointsCost,
        Boolean active,
        @PositiveOrZero Integer stock
) {
    public static RewardDto toDto(Reward reward) {
        return new RewardDto(
                reward.getId(),
                reward.getEstablishment().getId(),
                reward.getName(),
                reward.getDescription(),
                reward.getType(),
                reward.getPointsCost(),
                reward.isActive(),
                reward.getStock());
    }
}
