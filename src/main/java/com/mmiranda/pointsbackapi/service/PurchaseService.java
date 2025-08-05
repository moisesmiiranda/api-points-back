package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.PurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PurchaseService {
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EstablishmentRepository establishmentRepository;

    public List<PurchaseDto> listAllPurchases() {
        List<Purchase> purchases =  purchaseRepository.findAll();
        return purchases.stream()
                .map(PurchaseDto::toDto)
                .toList();
    }

    public void registerPurchase(PurchaseDto purchaseDto) {

        var establishment = establishmentRepository
            .findById(purchaseDto.establishmentId())
            .orElseThrow(() -> new RuntimeException("Cannot find establishment with id: " + purchaseDto.establishmentId()));

        Client client = clientRepository.findById(purchaseDto.clientId())
                .orElseThrow(() -> new RuntimeException("Cannot find client with id: " + purchaseDto.clientId()));

        Purchase purchase = new Purchase();
        purchase.setClient(client);
        purchase.setEstablishment(establishment);
        purchase.setAmount(purchaseDto.amount());

        int pointsEarned = calculatePoints(establishment.getValuePerPoint(), purchaseDto.amount());

        client.setPoints(client.getPoints() + pointsEarned);

        clientRepository.save(client);
        purchaseRepository.save(purchase);
    }

    private int calculatePoints(Integer valuePerPoints, BigDecimal purchaseValue) {
        return purchaseValue.divide(BigDecimal.valueOf(valuePerPoints), RoundingMode.HALF_DOWN).intValue();
    }
}
