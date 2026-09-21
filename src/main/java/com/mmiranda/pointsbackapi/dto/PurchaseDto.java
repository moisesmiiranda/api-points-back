package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A purchase. {@code amount} is the sale total before any discount. On requests, {@code redeemPoints}
 * is the number of points to pay part of it with; the other trailing fields are only filled in responses.
 */
public record PurchaseDto(
    Long purchaseId,
    @NotNull(groups = OnCreate.class) Long clientId,
    Long establishmentId,
    @NotNull(groups = OnCreate.class) @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
    @PositiveOrZero Integer redeemPoints,
    BigDecimal discountAmount,
    Integer pointsEarned,
    LocalDateTime createdAt,
    LocalDateTime cancelledAt
) {
    public PurchaseDto(Long purchaseId, Long clientId, Long establishmentId, BigDecimal amount) {
        this(purchaseId, clientId, establishmentId, amount, null, null, null, null, null);
    }

    public static PurchaseDto toDto(Purchase purchase) {
        return new PurchaseDto(
            purchase.getPurchaseId(),
            purchase.getClient().getId(),
            purchase.getEstablishment().getId(),
            purchase.getAmount(),
            purchase.getPointsRedeemed(),
            purchase.getDiscountAmount(),
            purchase.getPointsEarned(),
            purchase.getCreatedAt(),
            purchase.getCancelledAt()
        );
    }
    public static Purchase toEntity(PurchaseDto dto) {
        Purchase purchase = new Purchase();
        purchase.setPurchaseId(dto.purchaseId());
        purchase.setAmount(dto.amount());
        return purchase;
    }

}
