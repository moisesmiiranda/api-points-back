package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Purchase;

public record PurchaseDto(
        Long clientId,
        Long productId,
        Integer quantity
) {
    public static ProductDto toDto(Purchase purchase) {
        return new ProductDto(
                purchase.getClient().getId(),
                purchase.getProduct().getId(),
                purchase.getQuantity()
        );
    }
}

