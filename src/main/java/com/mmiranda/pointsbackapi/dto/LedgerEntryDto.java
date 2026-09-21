package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.LedgerEntry;
import com.mmiranda.pointsbackapi.model.PointsEntryType;

import java.time.LocalDateTime;

public record LedgerEntryDto(
        Long id,
        PointsEntryType type,
        int points,
        int balanceAfter,
        Long purchaseId,
        Long rewardId,
        String reason,
        Long createdBy,
        LocalDateTime createdAt
) {
    public static LedgerEntryDto toDto(LedgerEntry entry) {
        return new LedgerEntryDto(
                entry.getId(),
                entry.getType(),
                entry.getPoints(),
                entry.getBalanceAfter(),
                entry.getPurchase() != null ? entry.getPurchase().getPurchaseId() : null,
                entry.getReward() != null ? entry.getReward().getId() : null,
                entry.getReason(),
                entry.getCreatedBy(),
                entry.getCreatedAt());
    }
}
