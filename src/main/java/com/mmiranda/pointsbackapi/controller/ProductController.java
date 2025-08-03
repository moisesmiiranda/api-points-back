package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.ProductDto;
import com.mmiranda.pointsbackapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    @Autowired
    private ProductService productService;

    @PostMapping
    public ProductDto createProduct(@RequestBody ProductDto productDto) {
        return productService.createProduct(productDto);
    }

    @GetMapping("/establishment/{establishmentId}")
    public List<ProductDto> listProductsByEstablishment(@PathVariable Long establishmentId) {
        return productService.listProductsByEstablishment(establishmentId);
    }

}

