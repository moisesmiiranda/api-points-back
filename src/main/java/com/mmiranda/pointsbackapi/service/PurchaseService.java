package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Product;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.ProductRepository;
import com.mmiranda.pointsbackapi.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ProductRepository productRepository;

    public void registerPurchase(PurchaseDto purchaseDto) {
        Client client = clientRepository.findById(purchaseDto.clientId()).orElseThrow();
        Product product = productRepository.findById(purchaseDto.productId()).orElseThrow();

        Purchase purchase = new Purchase();
        purchase.setClient(client);
        purchase.setProduct(product);
        purchase.setQuantity(purchaseDto.quantity());
        purchase.setPurchaseDate(LocalDateTime.now());

        int pointsEarned = (product.getPointForSell() != null ? product.getPointForSell() : 0) * purchaseDto.quantity();
        client.setPoints((client.getPoints() != null ? client.getPoints() : 0) + pointsEarned);

        clientRepository.save(client);
        purchaseRepository.save(purchase);
    }

    public List<PurchaseDto> listAllPurchases() {
        return purchaseRepository.findAll()
                .stream()
                .map(PurchaseDto::toDto)
                .collect(Collectors.toList());
    }
}

