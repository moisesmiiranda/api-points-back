package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.RewardMode;

import java.math.BigDecimal;

/** What a client can pay with points on a purchase of a given amount. */
public record RedeemPreviewDto(
        int balance,
        BigDecimal balanceValue,
        RewardMode rewardMode,
        BigDecimal pointsToCurrencyRate,
        int maxPoints,
        BigDecimal maxDiscount
) {
}
