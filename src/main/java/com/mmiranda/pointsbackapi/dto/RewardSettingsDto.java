package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.RewardMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** How an establishment lets clients spend points. Rate and percentage keep their value when omitted. */
public record RewardSettingsDto(
        @NotNull RewardMode rewardMode,
        @Positive BigDecimal pointsToCurrencyRate,
        @Min(1) @Max(100) Integer maxDiscountPercent
) {
    public static RewardSettingsDto toDto(Establishment establishment) {
        return new RewardSettingsDto(
                establishment.getRewardMode(),
                establishment.getPointsToCurrencyRate(),
                establishment.getMaxDiscountPercent());
    }
}
