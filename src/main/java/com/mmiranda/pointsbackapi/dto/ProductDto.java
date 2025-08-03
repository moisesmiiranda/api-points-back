package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Product;
import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String description,
        // talvez só precise do pointForSell, mas vamos manter o price por enquanto
        BigDecimal price,
        Integer pointForSell,
        Long establishmentId
) {
    public static ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getDescription(),
                product.getPrice(),
                product.getPointForSell(),
                product.getEstablishment() != null ? product.getEstablishment().getId() : null
        );
    }

    public static Product toEntity(ProductDto dto) {
        Product product = new Product();
        product.setId(dto.id());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setPointForSell(dto.pointForSell());
        // O estabelecimento deve ser setado no service
        return product;
    }
}

