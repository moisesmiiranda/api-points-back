package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.InsufficientPointsException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.LedgerRepository;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.LedgerEntry;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.exception.ConflictException;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.PurchaseRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
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

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private PurchaseService purchaseService;

    private Purchase purchaseTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        purchaseTest = buildPurchase();
        TestAuth.asPlatformAdmin();
        // The real PointsService on the mocked repositories, so balances and ledger entries are exercised for real
        ReflectionTestUtils.setField(purchaseService, "pointsService", new PointsService(clientRepository, ledgerRepository));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
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
    void listAllPurchasesScopedForNonAdmin() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentOwner(1L);

        when(purchaseRepository.findAllByEstablishmentId(1L)).thenReturn(Collections.singletonList(purchaseTest));

        // Act
        List<PurchaseDto> result = purchaseService.listAllPurchases();

        // Assert
        assertEquals(1, result.size());
        verify(purchaseRepository, times(1)).findAllByEstablishmentId(1L);
        verify(purchaseRepository, never()).findAll();
    }

    @Test
    void registerPurchase() {
        PurchaseDto purchaseDto = new PurchaseDto(1L, 1L, 1L, BigDecimal.valueOf(100));
        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setValuePerPoint(10);
        clientTest.setPoints(50);
        clientTest.setEstablishment(establishment);

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));

        purchaseService.registerPurchase(purchaseDto);

        assertEquals(60, clientTest.getPoints());
        verify(clientRepository, times(1)).save(clientTest);
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    void registerPurchaseForbiddenForDifferentEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);
        PurchaseDto purchaseDto = new PurchaseDto(null, 1L, 1L, BigDecimal.valueOf(100));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> purchaseService.registerPurchase(purchaseDto));
        verify(establishmentRepository, never()).findById(any());
    }

    @Test
    void registerPurchaseDefaultsToCallerEstablishmentWhenNotProvided() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        PurchaseDto purchaseDto = new PurchaseDto(null, 1L, null, BigDecimal.valueOf(100));

        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setValuePerPoint(10);
        clientTest.setPoints(50);
        clientTest.setEstablishment(establishment);

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));

        purchaseService.registerPurchase(purchaseDto);

        assertEquals(60, clientTest.getPoints());
        verify(purchaseRepository, times(1)).save(any(Purchase.class));
    }

    @Test
    void getPurchaseByIdForbiddenForOtherEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);

        when(purchaseRepository.findByPurchaseIdAndEstablishmentId(1L, 2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> purchaseService.getPurchaseById(1L));
    }

    @Test
    void getPurchaseByIdScopedToOwnEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);

        when(purchaseRepository.findByPurchaseIdAndEstablishmentId(1L, 1L)).thenReturn(Optional.of(purchaseTest));

        // Act
        PurchaseDto result = purchaseService.getPurchaseById(1L);

        // Assert
        assertNotNull(result);
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
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> purchaseService.registerPurchase(purchaseDto));
    }

    @Test
    void registerPurchase_clientOfAnotherEstablishmentIsRejected() {
        PurchaseDto purchaseDto = new PurchaseDto(null, 1L, 1L, BigDecimal.valueOf(100));
        Establishment establishment = establishment(1L, 10);
        Client foreignClient = client(1L, 50, establishment(2L, 10));

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(foreignClient));

        assertThrows(ResourceNotFoundException.class, () -> purchaseService.registerPurchase(purchaseDto));

        assertEquals(50, foreignClient.getPoints());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void registerPurchase_treatsMissingPointsAsZero() {
        PurchaseDto purchaseDto = new PurchaseDto(null, 1L, 1L, BigDecimal.valueOf(100));
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 0, establishment);
        client.setPoints(null);

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));

        purchaseService.registerPurchase(purchaseDto);

        assertEquals(10, client.getPoints());
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

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> purchaseService.getPurchaseById(purchaseId));
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
                .person(com.mmiranda.pointsbackapi.model.Person.builder().cpf("55555555555").build())
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
    void updatePurchaseById_AmountChangeAdjustsPointsOfSameClient() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);

        PurchaseDto result = purchaseService.updatePurchaseById(1L,
                new PurchaseDto(null, null, null, new BigDecimal("150.00")));

        assertEquals(new BigDecimal("150.00"), result.amount());
        assertEquals(105, client.getPoints());
        verify(purchaseRepository, times(1)).save(existing);
    }

    @Test
    void updatePurchaseById_AmountDecreaseTakesPointsBack() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);

        purchaseService.updatePurchaseById(1L, new PurchaseDto(null, null, null, new BigDecimal("50.00")));

        assertEquals(95, client.getPoints());
    }

    @Test
    void updatePurchaseById_PurchaseNotFound() {
        when(purchaseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> purchaseService.updatePurchaseById(999L,
                new PurchaseDto(null, null, null, new BigDecimal("150.00"))));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_MovesPointsToNewClientOfSameEstablishment() {
        Establishment establishment = establishment(1L, 10);
        Client oldClient = client(1L, 100, establishment);
        Client newClient = client(2L, 50, establishment);
        Purchase existing = purchase(1L, oldClient, establishment, "100.00");
        stubUpdate(existing, oldClient);
        when(clientRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(newClient));

        PurchaseDto result = purchaseService.updatePurchaseById(1L, new PurchaseDto(null, 2L, null, null));

        assertEquals(2L, result.clientId());
        assertEquals(90, oldClient.getPoints());
        assertEquals(60, newClient.getPoints());
    }

    @Test
    void updatePurchaseById_ClientFromAnotherEstablishmentIsRejected() {
        Establishment establishment = establishment(1L, 10);
        Client oldClient = client(1L, 100, establishment);
        Client foreignClient = client(2L, 50, establishment(2L, 10));
        Purchase existing = purchase(1L, oldClient, establishment, "100.00");
        stubUpdate(existing, oldClient);
        when(clientRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(foreignClient));

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseService.updatePurchaseById(1L, new PurchaseDto(null, 2L, null, null)));

        assertEquals(100, oldClient.getPoints());
        assertEquals(50, foreignClient.getPoints());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_NonAdminCannotAttachAnotherEstablishmentsClient() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        Establishment establishment = establishment(1L, 10);
        Client oldClient = client(1L, 100, establishment);
        Client foreignClient = client(9L, 50, establishment(2L, 10));
        Purchase existing = purchase(1L, oldClient, establishment, "100.00");
        when(purchaseRepository.findByPurchaseIdAndEstablishmentId(1L, 1L)).thenReturn(Optional.of(existing));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(oldClient));
        when(clientRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(foreignClient));

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseService.updatePurchaseById(1L, new PurchaseDto(null, 9L, null, null)));

        assertEquals(50, foreignClient.getPoints());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_NonAdminCannotMoveToAnotherEstablishment() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        when(purchaseRepository.findByPurchaseIdAndEstablishmentId(1L, 1L)).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class,
                () -> purchaseService.updatePurchaseById(1L, new PurchaseDto(null, null, 2L, null)));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_ClientNotFound() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);
        when(clientRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseService.updatePurchaseById(1L, new PurchaseDto(null, 999L, null, null)));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_EstablishmentNotFound() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> purchaseService.updatePurchaseById(1L, new PurchaseDto(null, null, 999L, null)));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_MovesToAnotherEstablishmentRecalculatingPoints() {
        Establishment oldEstablishment = establishment(1L, 10);
        Establishment newEstablishment = establishment(2L, 20);
        Client oldClient = client(1L, 100, oldEstablishment);
        Client newClient = client(2L, 50, newEstablishment);
        Purchase existing = purchase(1L, oldClient, oldEstablishment, "100.00");
        stubUpdate(existing, oldClient);
        when(clientRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(newClient));
        when(establishmentRepository.findById(2L)).thenReturn(Optional.of(newEstablishment));

        PurchaseDto result = purchaseService.updatePurchaseById(1L,
                new PurchaseDto(null, 2L, 2L, new BigDecimal("300.00")));

        assertEquals(2L, result.clientId());
        assertEquals(2L, result.establishmentId());
        assertEquals(90, oldClient.getPoints());
        assertEquals(65, newClient.getPoints());
    }

    @Test
    void updatePurchaseById_RejectsChangeThatWouldMakeBalanceNegative() {
        Establishment establishment = establishment(1L, 10);
        Client oldClient = client(1L, 5, establishment);
        Client newClient = client(2L, 50, establishment);
        Purchase existing = purchase(1L, oldClient, establishment, "100.00");
        stubUpdate(existing, oldClient);
        when(clientRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(newClient));

        assertThrows(InsufficientPointsException.class,
                () -> purchaseService.updatePurchaseById(1L, new PurchaseDto(null, 2L, null, null)));

        assertEquals(5, oldClient.getPoints());
        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void updatePurchaseById_TreatsMissingPointsAsZero() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 0, establishment);
        client.setPoints(null);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);

        purchaseService.updatePurchaseById(1L, new PurchaseDto(null, null, null, new BigDecimal("200.00")));

        assertEquals(10, client.getPoints());
    }

    // ------------------------------------------------------------------ ledger, discounts, cancellation (mvp-03)

    private Client clientWithBalance(int points, Establishment establishment) {
        Client client = client(1L, points, establishment);
        when(establishmentRepository.findById(establishment.getId())).thenReturn(Optional.of(establishment));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));
        return client;
    }

    private List<LedgerEntry> savedLedgerEntries() {
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void registerPurchase_writesACreditEntryWithTheRunningBalance() {
        Establishment establishment = establishment(1L, 10);
        Client client = clientWithBalance(50, establishment);

        PurchaseDto result = purchaseService.registerPurchase(new PurchaseDto(null, 1L, 1L, new BigDecimal("100.00")));

        assertEquals(60, client.getPoints());
        assertEquals(10, result.pointsEarned());
        assertEquals(0, result.redeemPoints());
        List<LedgerEntry> entries = savedLedgerEntries();
        assertEquals(1, entries.size());
        assertEquals(PointsEntryType.CREDIT, entries.get(0).getType());
        assertEquals(10, entries.get(0).getPoints());
        assertEquals(60, entries.get(0).getBalanceAfter());
    }

    @Test
    void registerPurchase_earnsPointsOnTheAmountActuallyPaidAfterTheDiscount() {
        Establishment establishment = establishment(1L, 10);
        establishment.setRewardMode(RewardMode.DISCOUNT);
        establishment.setPointsToCurrencyRate(new BigDecimal("0.1000"));
        establishment.setMaxDiscountPercent(50);
        Client client = clientWithBalance(300, establishment);

        // 200 points x 0.10 = 20.00 off a 100.00 sale: pays 80.00, which earns 8 points
        PurchaseDto result = purchaseService.registerPurchase(
                new PurchaseDto(null, 1L, 1L, new BigDecimal("100.00"), 200, null, null, null, null));

        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals(200, result.redeemPoints());
        assertEquals(8, result.pointsEarned());
        assertEquals(300 - 200 + 8, client.getPoints());
        List<LedgerEntry> entries = savedLedgerEntries();
        assertEquals(PointsEntryType.REDEEM, entries.get(0).getType());
        assertEquals(-200, entries.get(0).getPoints());
        assertEquals(PointsEntryType.CREDIT, entries.get(1).getType());
        assertEquals(108, entries.get(1).getBalanceAfter());
    }

    @Test
    void registerPurchase_paidEntirelyWithPointsEarnsNothing() {
        Establishment establishment = establishment(1L, 10);
        establishment.setRewardMode(RewardMode.CASHBACK);
        establishment.setMaxDiscountPercent(100);
        Client client = clientWithBalance(1000, establishment);

        PurchaseDto result = purchaseService.registerPurchase(
                new PurchaseDto(null, 1L, 1L, new BigDecimal("50.00"), 500, null, null, null, null));

        assertEquals(0, result.pointsEarned());
        assertEquals(500, client.getPoints());
        assertEquals(1, savedLedgerEntries().size());
    }

    @Test
    void registerPurchase_rejectsRedeemingPointsInCatalogMode() {
        Establishment establishment = establishment(1L, 10);
        clientWithBalance(300, establishment);

        assertThrows(ConflictException.class, () -> purchaseService.registerPurchase(
                new PurchaseDto(null, 1L, 1L, new BigDecimal("100.00"), 10, null, null, null, null)));

        verify(purchaseRepository, never()).save(any(Purchase.class));
    }

    @Test
    void registerPurchase_rejectsADiscountAboveTheMaximumShare() {
        Establishment establishment = establishment(1L, 10);
        establishment.setRewardMode(RewardMode.DISCOUNT);
        establishment.setMaxDiscountPercent(30);
        clientWithBalance(1000, establishment);

        // 301 points = 30.10 off a 100.00 sale, above 30%
        assertThrows(IllegalArgumentException.class, () -> purchaseService.registerPurchase(
                new PurchaseDto(null, 1L, 1L, new BigDecimal("100.00"), 301, null, null, null, null)));
        // exactly 30% is fine
        purchaseService.registerPurchase(
                new PurchaseDto(null, 1L, 1L, new BigDecimal("100.00"), 300, null, null, null, null));
    }

    @Test
    void registerPurchase_rejectsRedeemingMoreThanTheBalance() {
        Establishment establishment = establishment(1L, 10);
        establishment.setRewardMode(RewardMode.DISCOUNT);
        Client client = clientWithBalance(50, establishment);

        assertThrows(InsufficientPointsException.class, () -> purchaseService.registerPurchase(
                new PurchaseDto(null, 1L, 1L, new BigDecimal("100.00"), 100, null, null, null, null)));

        assertEquals(50, client.getPoints());
    }

    @Test
    void cancelPurchase_takesBackTheEarnedPointsAndMarksItCancelled() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 30, establishment);
        Purchase purchase = purchase(1L, client, establishment, "100.00");
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));

        PurchaseDto result = purchaseService.cancelPurchase(1L);

        assertEquals(20, client.getPoints());
        assertNotNull(result.cancelledAt());
        List<LedgerEntry> entries = savedLedgerEntries();
        assertEquals(PointsEntryType.REVERSAL, entries.get(0).getType());
        assertEquals(-10, entries.get(0).getPoints());
    }

    @Test
    void cancelPurchase_returnsRedeemedPointsBeforeRemovingEarnedOnes() {
        Establishment establishment = establishment(1L, 10);
        // balance 5 is smaller than the 8 points this purchase earned; the 200 redeemed points come back first
        Client client = client(1L, 5, establishment);
        Purchase purchase = purchase(1L, client, establishment, "100.00");
        purchase.setPointsRedeemed(200);
        purchase.setDiscountAmount(new BigDecimal("20.00"));
        purchase.setPointsEarned(8);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));

        purchaseService.cancelPurchase(1L);

        assertEquals(5 + 200 - 8, client.getPoints());
        List<LedgerEntry> entries = savedLedgerEntries();
        assertEquals(200, entries.get(0).getPoints());
        assertEquals(-8, entries.get(1).getPoints());
    }

    @Test
    void cancelPurchase_isRefusedWhenTheEarnedPointsWereAlreadySpent() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 3, establishment);
        Purchase purchase = purchase(1L, client, establishment, "100.00");
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(client));

        assertThrows(InsufficientPointsException.class, () -> purchaseService.cancelPurchase(1L));

        assertEquals(3, client.getPoints());
        org.junit.jupiter.api.Assertions.assertNull(purchase.getCancelledAt());
    }

    @Test
    void cancelPurchase_cannotBeDoneTwiceAndIsScopedToTheCallersEstablishment() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 30, establishment);
        Purchase purchase = purchase(1L, client, establishment, "100.00");
        purchase.setCancelledAt(java.time.LocalDateTime.now());
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));

        assertThrows(ConflictException.class, () -> purchaseService.cancelPurchase(1L));

        TestAuth.clear();
        TestAuth.asEstablishmentOwner(2L);
        when(purchaseRepository.findByPurchaseIdAndEstablishmentId(1L, 2L)).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class, () -> purchaseService.cancelPurchase(1L));
    }

    @Test
    void cancelPurchase_missingClientIsNotFound() {
        Establishment establishment = establishment(1L, 10);
        Purchase purchase = purchase(1L, client(1L, 30, establishment), establishment, "100.00");
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(purchase));
        when(clientRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> purchaseService.cancelPurchase(1L));
    }

    @Test
    void updatePurchaseById_isRefusedForCancelledOrPointsPaidPurchases() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase cancelled = purchase(1L, client, establishment, "100.00");
        cancelled.setCancelledAt(java.time.LocalDateTime.now());
        Purchase paidWithPoints = purchase(2L, client, establishment, "100.00");
        paidWithPoints.setPointsRedeemed(10);
        when(purchaseRepository.findById(1L)).thenReturn(Optional.of(cancelled));
        when(purchaseRepository.findById(2L)).thenReturn(Optional.of(paidWithPoints));

        assertThrows(ConflictException.class, () -> purchaseService.updatePurchaseById(1L,
                new PurchaseDto(null, null, null, new BigDecimal("50.00"))));
        assertThrows(ConflictException.class, () -> purchaseService.updatePurchaseById(2L,
                new PurchaseDto(null, null, null, new BigDecimal("50.00"))));
    }

    @Test
    void updatePurchaseById_writesCreditAndReversalEntriesAndKeepsPointsEarnedInSync() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);

        PurchaseDto result = purchaseService.updatePurchaseById(1L,
                new PurchaseDto(null, null, null, new BigDecimal("150.00")));

        assertEquals(15, result.pointsEarned());
        List<LedgerEntry> entries = savedLedgerEntries();
        assertEquals(2, entries.size());
        assertEquals(PointsEntryType.CREDIT, entries.get(0).getType());
        assertEquals(15, entries.get(0).getPoints());
        assertEquals(PointsEntryType.REVERSAL, entries.get(1).getType());
        assertEquals(-10, entries.get(1).getPoints());
        assertEquals(105, entries.get(1).getBalanceAfter());
    }

    @Test
    void updatePurchaseById_withNoChangeInPointsWritesNothing() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);

        purchaseService.updatePurchaseById(1L, new PurchaseDto(null, null, null, new BigDecimal("105.00")));

        assertEquals(100, client.getPoints());
        assertEquals(0, savedLedgerEntries().size());
    }

    @Test
    void updatePurchaseById_amountBelowTheValuePerPointEarnsNoPoints() {
        Establishment establishment = establishment(1L, 10);
        Client client = client(1L, 100, establishment);
        Purchase existing = purchase(1L, client, establishment, "100.00");
        stubUpdate(existing, client);

        PurchaseDto result = purchaseService.updatePurchaseById(1L,
                new PurchaseDto(null, null, null, new BigDecimal("5.00")));

        assertEquals(0, result.pointsEarned());
        assertEquals(90, client.getPoints());
    }

    @Test
    void pointsAreFlooredNotRounded() {
        Establishment establishment = establishment(1L, 10);
        Client client = clientWithBalance(0, establishment);

        purchaseService.registerPurchase(new PurchaseDto(null, 1L, 1L, new BigDecimal("99.99")));

        assertEquals(9, client.getPoints());
    }


    private void stubUpdate(Purchase existing, Client lockedClient) {
        when(purchaseRepository.findById(existing.getPurchaseId())).thenReturn(Optional.of(existing));
        when(clientRepository.findByIdForUpdate(lockedClient.getId())).thenReturn(Optional.of(lockedClient));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Establishment establishment(Long id, int valuePerPoint) {
        Establishment establishment = new Establishment();
        establishment.setId(id);
        establishment.setValuePerPoint(valuePerPoint);
        return establishment;
    }

    private static Client client(Long id, int points, Establishment establishment) {
        return Client.builder().id(id).name("Client " + id).points(points).establishment(establishment).build();
    }

    private static Purchase purchase(Long id, Client client, Establishment establishment, String amount) {
        Purchase purchase = new Purchase();
        purchase.setPurchaseId(id);
        purchase.setClient(client);
        purchase.setEstablishment(establishment);
        purchase.setAmount(new BigDecimal(amount));
        if (establishment.getValuePerPoint() != null) {
            purchase.setPointsEarned(RewardCalculator.pointsFor(establishment.getValuePerPoint(), new BigDecimal(amount)));
        }
        return purchase;
    }

    public Client buildClient() {
        return Client.builder()
                .id(1L)
                .name("Test Client")
                .email("test@example.com")
                .phone("1234567890")
                .person(com.mmiranda.pointsbackapi.model.Person.builder().cpf("12345678900").build())
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
