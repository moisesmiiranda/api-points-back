package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.ProductDto;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Product;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private EstablishmentRepository establishmentRepository;

    public ProductDto createProduct(ProductDto productDto) {
        Product product = ProductDto.toEntity(productDto);
        if (productDto.establishmentId() != null) {
            Establishment establishment = establishmentRepository.findById(productDto.establishmentId()).orElse(null);
            product.setEstablishment(establishment);
        }
        return ProductDto.toDto(productRepository.save(product));
    }

    public List<ProductDto> listProductsByEstablishment(Long establishmentId) {
        return productRepository.findByEstablishmentId(establishmentId)
                .stream()
                .map(ProductDto::toDto)
                .collect(Collectors.toList());
    }
}

