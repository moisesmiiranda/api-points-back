package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EstablishmentRepository establishmentRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    Client clientTest = buildClient();

    @Test
    void listAllPurchases() {
        // Arrange
        Establishment establishment = new Establishment();
        Purchase purchase = new Purchase();
        purchase.setPurchaseId(1L);
        purchase.setClient(clientTest);
        purchase.setEstablishment(establishment);
        purchase.setAmount(new BigDecimal("100.00"));

        when(purchaseRepository.findAll()).thenReturn(Collections.singletonList(purchase));

        // Act
        List<PurchaseDto> result = purchaseService.listAllPurchases();

        // Assert
        assertEquals(1, result.size());
        verify(purchaseRepository, times(1)).findAll();
    }

    @Test
    void registerPurchase() {
        PurchaseDto purchaseDto = new PurchaseDto(1L, 1L, 1L, BigDecimal.valueOf(100));
        Establishment establishment = new Establishment();
        establishment.setValuePerPoint(10);        
        clientTest.setPoints(50);

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(clientRepository.findById(1L)).thenReturn(Optional.of(clientTest));

        purchaseService.registerPurchase(purchaseDto);

        assertEquals(60, clientTest.getPoints());
        verify(clientRepository, times(1)).save(clientTest);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    void registerPurchase_establishmentNotFound() {
        PurchaseDto purchaseDto = new PurchaseDto(1L, 1L, 1L, BigDecimal.valueOf(100));

        when(establishmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> purchaseService.registerPurchase(purchaseDto));
    }

    @Test
    void registerPurchase_clientNotFound() {
        PurchaseDto purchaseDto = new PurchaseDto(1L, 1L, 1L, BigDecimal.valueOf(100));
        Establishment establishment = new Establishment();
        establishment.setValuePerPoint(10);

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> purchaseService.registerPurchase(purchaseDto));
    }


    public Client buildClient() {
        return Client.builder()
                .id(1L)
                .name("Test Client")
                .email("test@example.com")
                .phone("1234567890")
                .cpf("123.456.789-00")
                .points(100)
                .build();
    }
}
