package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Purchase;

import java.math.BigDecimal;

public record PurchaseDto(
    Long purchaseId,
    Long clientId,
    Long establishmentId,
    BigDecimal amount
) {
    public static PurchaseDto toDto(Purchase purchase) {
        return new PurchaseDto(
            purchase.getPurchaseId(),
            purchase.getClient().getId(),
            purchase.getEstablishment().getId(),
            purchase.getAmount()
        );
    }

}

