package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Redemption;
import com.mmiranda.pointsbackapi.model.RedemptionStatus;

import java.time.LocalDateTime;

public record RedemptionDto(
        Long id,
        Long clientId,
        String clientName,
        Long rewardId,
        String rewardName,
        int pointsCost,
        String code,
        RedemptionStatus status,
        LocalDateTime createdAt,
        LocalDateTime usedAt
) {
    public static RedemptionDto toDto(Redemption redemption) {
        return new RedemptionDto(
                redemption.getId(),
                redemption.getClient().getId(),
                redemption.getClient().getName(),
                redemption.getReward().getId(),
                redemption.getReward().getName(),
                redemption.getPointsCost(),
                redemption.getCode(),
                redemption.getStatus(),
                redemption.getCreatedAt(),
                redemption.getUsedAt());
    }
}
