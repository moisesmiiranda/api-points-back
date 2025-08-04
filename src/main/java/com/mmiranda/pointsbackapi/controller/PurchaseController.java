package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    @PostMapping
    public void registerPurchase(@RequestBody PurchaseDto purchaseDto) {
        purchaseService.registerPurchase(purchaseDto);
    }

    @GetMapping
    public List<PurchaseDto> listAllPurchases() {
        return purchaseService.listAllPurchases();
    }
}

