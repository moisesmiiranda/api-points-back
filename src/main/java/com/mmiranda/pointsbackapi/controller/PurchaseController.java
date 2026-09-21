package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.service.PurchaseService;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.List;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    @PostMapping
    public PurchaseDto registerPurchase(@Validated(OnCreate.class) @RequestBody PurchaseDto purchaseDto) {
        return purchaseService.registerPurchase(purchaseDto);
    }

    @GetMapping
    public List<PurchaseDto> listAllPurchases() {
        return purchaseService.listAllPurchases();
    }

    @GetMapping("/{id}")
    public PurchaseDto getPurchaseById(@PathVariable Long id) {
        return purchaseService.getPurchaseById(id);
    }
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')")
    public PurchaseDto cancelPurchase(@PathVariable Long id) {
        return purchaseService.cancelPurchase(id);
    }

    @PutMapping("/{id}")
    public PurchaseDto updatePurchaseById(@PathVariable Long id, @Valid @RequestBody PurchaseDto purchaseDto) {
        return purchaseService.updatePurchaseById(id, purchaseDto);
    }
}
