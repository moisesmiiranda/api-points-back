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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class PurchaseServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EstablishmentRepository establishmentRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    private Purchase purchaseTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        purchaseTest = buildPurchase();
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

    @Test
    void testGetPurchaseById() {
      // Arrange
        Long purchaseId = 1L;

        when(purchaseRepository.findById(purchaseId)).thenReturn(Optional.of(purchaseTest));

        // Act
        PurchaseDto result = purchaseService.getPurchaseById(purchaseId);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(150.00), result.amount());
        verify(purchaseRepository, times(1)).findById(purchaseId);
    }

    @Test
    void testGetPurchaseByIdNotFound() {
        // Arrange
        Long purchaseId = 999L;

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.empty());

        // Act
        PurchaseDto result = purchaseService.getPurchaseById(purchaseId);

        // Assert
        assertNull(result);
        verify(purchaseRepository, times(1)).findById(purchaseId);
    }

    @Test
    void testGetPurchaseByIdWithValidData() {
        // Arrange
        Long purchaseId = 1L;

        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setName("Test Establishment");

        Purchase expectedPurchase = new Purchase();
        expectedPurchase.setPurchaseId(1L);
        expectedPurchase.setClient(clientTest);
        expectedPurchase.setEstablishment(establishment);
        expectedPurchase.setAmount(BigDecimal.valueOf(250.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(expectedPurchase));

        // Act
        PurchaseDto result = purchaseService.getPurchaseById(purchaseId);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.purchaseId());
        assertEquals(1L, result.clientId());
        assertEquals(1L, result.establishmentId());
        assertEquals(BigDecimal.valueOf(250.00), result.amount());
        verify(purchaseRepository, times(1)).findById(purchaseId);
    }

    @Test
    void testGetPurchaseByIdWithDifferentPurchaseId() {
        // Arrange
        Long purchaseId = 5L;

        Client differentClient = Client.builder()
                .id(3L)
                .name("Different Client")
                .email("different@example.com")
                .phone("5555555555")
                .cpf("555.555.555-55")
                .points(200)
                .build();

        Establishment differentEstablishment = new Establishment();
        differentEstablishment.setId(3L);
        differentEstablishment.setName("Different Establishment");

        Purchase expectedPurchase = new Purchase();
        expectedPurchase.setPurchaseId(5L);
        expectedPurchase.setClient(differentClient);
        expectedPurchase.setEstablishment(differentEstablishment);
        expectedPurchase.setAmount(BigDecimal.valueOf(500.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(expectedPurchase));

        // Act
        PurchaseDto result = purchaseService.getPurchaseById(purchaseId);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.purchaseId());
        assertEquals(3L, result.clientId());
        assertEquals(3L, result.establishmentId());
        assertEquals(BigDecimal.valueOf(500.00), result.amount());
        verify(purchaseRepository, times(1)).findById(purchaseId);
    }

    @Test
    void updatePurchaseById_Success() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            null,
            null,
            BigDecimal.valueOf(150.00)
        );

        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setValuePerPoint(10);

        Purchase existingPurchase = new Purchase();
        existingPurchase.setPurchaseId(1L);
        existingPurchase.setClient(clientTest);
        existingPurchase.setEstablishment(establishment);
        existingPurchase.setAmount(BigDecimal.valueOf(100.00));

        Purchase updatedPurchase = new Purchase();
        updatedPurchase.setPurchaseId(1L);
        updatedPurchase.setClient(clientTest);
        updatedPurchase.setEstablishment(establishment);
        updatedPurchase.setAmount(BigDecimal.valueOf(150.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(existingPurchase));
        when(purchaseRepository.save(any(Purchase.class)))
                .thenReturn(updatedPurchase);

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(BigDecimal.valueOf(150.00), result.amount());
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_PurchaseNotFound() {
        // Arrange
        Long purchaseId = 999L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            null,
            null,
            BigDecimal.valueOf(150.00)
        );

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.empty());

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(null, result);
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_UpdateAmount() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            null,
            null,
            BigDecimal.valueOf(200.00)
        );

        Establishment establishment = new Establishment();
        establishment.setId(1L);

        Purchase existingPurchase = new Purchase();
        existingPurchase.setPurchaseId(1L);
        existingPurchase.setClient(clientTest);
        existingPurchase.setEstablishment(establishment);
        existingPurchase.setAmount(BigDecimal.valueOf(100.00));

        Purchase updatedPurchase = new Purchase();
        updatedPurchase.setPurchaseId(1L);
        updatedPurchase.setClient(clientTest);
        updatedPurchase.setEstablishment(establishment);
        updatedPurchase.setAmount(BigDecimal.valueOf(200.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(existingPurchase));
        when(purchaseRepository.save(any(Purchase.class)))
                .thenReturn(updatedPurchase);

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(BigDecimal.valueOf(200.00), result.amount());
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_UpdateClientAndEstablishment() {
        // Arrange
        Long purchaseId = 1L;
        
        Client newClient = Client.builder()
                .id(2L)
                .name("New Client")
                .email("new@example.com")
                .phone("9876543210")
                .cpf("987.654.321-00")
                .points(50)
                .build();

        Establishment oldEstablishment = new Establishment();
        oldEstablishment.setId(1L);

        Establishment newEstablishment = new Establishment();
        newEstablishment.setId(2L);

        PurchaseDto updateDto = new PurchaseDto(
            null,
            2L,
            2L,
            null
        );

        Purchase existingPurchase = new Purchase();
        existingPurchase.setPurchaseId(1L);
        existingPurchase.setClient(clientTest);
        existingPurchase.setEstablishment(oldEstablishment);
        existingPurchase.setAmount(BigDecimal.valueOf(100.00));

        Purchase updatedPurchase = new Purchase();
        updatedPurchase.setPurchaseId(1L);
        updatedPurchase.setClient(newClient);
        updatedPurchase.setEstablishment(newEstablishment);
        updatedPurchase.setAmount(BigDecimal.valueOf(100.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(existingPurchase));
        when(clientRepository.findById(2L))
                .thenReturn(Optional.of(newClient));
        when(establishmentRepository.findById(2L))
                .thenReturn(Optional.of(newEstablishment));
        when(purchaseRepository.save(any(Purchase.class)))
                .thenReturn(updatedPurchase);

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(2L, result.clientId());
        assertEquals(2L, result.establishmentId());
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(clientRepository, times(1)).findById(2L);
        verify(establishmentRepository, times(1)).findById(2L);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_UpdateClientNotFound() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            999L,
            null,
            null
        );

        Establishment establishment = new Establishment();
        establishment.setId(1L);

        Purchase existingPurchase = new Purchase();
        existingPurchase.setPurchaseId(1L);
        existingPurchase.setClient(clientTest);
        existingPurchase.setEstablishment(establishment);
        existingPurchase.setAmount(BigDecimal.valueOf(100.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(existingPurchase));
        when(clientRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(null, result);
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(clientRepository, times(1)).findById(999L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_UpdateEstablishmentNotFound() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            null,
            999L,
            null
        );

        Establishment establishment = new Establishment();
        establishment.setId(1L);

        Purchase existingPurchase = new Purchase();
        existingPurchase.setPurchaseId(1L);
        existingPurchase.setClient(clientTest);
        existingPurchase.setEstablishment(establishment);
        existingPurchase.setAmount(BigDecimal.valueOf(100.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(existingPurchase));
        when(establishmentRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(null, result);
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(establishmentRepository, times(1)).findById(999L);
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_UpdateAllFields() {
        // Arrange
        Long purchaseId = 1L;
        
        Client newClient = Client.builder()
                .id(2L)
                .name("New Client")
                .email("new@example.com")
                .phone("9876543210")
                .cpf("987.654.321-00")
                .points(50)
                .build();

        Establishment oldEstablishment = new Establishment();
        oldEstablishment.setId(1L);

        Establishment newEstablishment = new Establishment();
        newEstablishment.setId(2L);

        PurchaseDto updateDto = new PurchaseDto(
            null,
            2L,
            2L,
            BigDecimal.valueOf(300.00)
        );

        Purchase existingPurchase = new Purchase();
        existingPurchase.setPurchaseId(1L);
        existingPurchase.setClient(clientTest);
        existingPurchase.setEstablishment(oldEstablishment);
        existingPurchase.setAmount(BigDecimal.valueOf(100.00));

        Purchase updatedPurchase = new Purchase();
        updatedPurchase.setPurchaseId(1L);
        updatedPurchase.setClient(newClient);
        updatedPurchase.setEstablishment(newEstablishment);
        updatedPurchase.setAmount(BigDecimal.valueOf(300.00));

        when(purchaseRepository.findById(purchaseId))
                .thenReturn(Optional.of(existingPurchase));
        when(clientRepository.findById(2L))
                .thenReturn(Optional.of(newClient));
        when(establishmentRepository.findById(2L))
                .thenReturn(Optional.of(newEstablishment));
        when(purchaseRepository.save(any(Purchase.class)))
                .thenReturn(updatedPurchase);

        // Act
        PurchaseDto result = purchaseService.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(2L, result.clientId());
        assertEquals(2L, result.establishmentId());
        assertEquals(BigDecimal.valueOf(300.00), result.amount());
        verify(purchaseRepository, times(1)).findById(purchaseId);
        verify(clientRepository, times(1)).findById(2L);
        verify(establishmentRepository, times(1)).findById(2L);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
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

    public Purchase buildPurchase() {
        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setName("Test Establishment");
        establishment.setValuePerPoint(10);

        Purchase purchase = new Purchase();
        purchase.setPurchaseId(1L);
        purchase.setClient(buildClient());
        purchase.setEstablishment(establishment);
        purchase.setAmount(BigDecimal.valueOf(150.00));
        return purchase;
    }
}
