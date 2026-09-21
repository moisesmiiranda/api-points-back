package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.RedemptionDto;
import com.mmiranda.pointsbackapi.dto.RedemptionRequestDto;
import com.mmiranda.pointsbackapi.exception.ConflictException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.InsufficientPointsException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.LedgerEntry;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.Redemption;
import com.mmiranda.pointsbackapi.model.RedemptionStatus;
import com.mmiranda.pointsbackapi.model.Reward;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.model.RewardType;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.LedgerRepository;
import com.mmiranda.pointsbackapi.repository.RedemptionRepository;
import com.mmiranda.pointsbackapi.repository.RewardRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private RedemptionRepository redemptionRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    private RedemptionService service;
    private Establishment establishment;
    private Client client;
    private Reward reward;

    @BeforeEach
    void setUp() {
        service = new RedemptionService(clientRepository, rewardRepository, redemptionRepository,
                new PointsService(clientRepository, ledgerRepository));
        establishment = new Establishment();
        establishment.setId(1L);
        client = Client.builder().id(10L).name("Ana").points(200).establishment(establishment).build();
        reward = Reward.builder().id(5L).establishment(establishment).name("Coffee").type(RewardType.BRINDE)
                .pointsCost(50).active(true).stock(2).build();
        TestAuth.asEstablishmentStaff(1L);
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    private void stubHappyPath() {
        // lenient: tests that are refused early never reach the later collaborators
        lenient().when(clientRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(client));
        lenient().when(rewardRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(reward));
        lenient().when(redemptionRepository.save(any(Redemption.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void redeemChargesThePointsTakesOneFromStockAndIssuesAVoucher() {
        stubHappyPath();

        RedemptionDto result = service.redeem(new RedemptionRequestDto(10L, 5L));

        assertEquals(150, client.getPoints());
        assertEquals(1, reward.getStock());
        assertEquals(50, result.pointsCost());
        assertEquals(RedemptionStatus.ISSUED, result.status());
        assertEquals(8, result.code().length());
        assertTrue(result.code().matches("[A-HJ-NP-Z2-9]{8}"));
        ArgumentCaptor<LedgerEntry> entry = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(entry.capture());
        assertEquals(PointsEntryType.REDEEM, entry.getValue().getType());
        assertEquals(-50, entry.getValue().getPoints());
        assertEquals(reward, entry.getValue().getReward());
    }

    @Test
    void anUnlimitedRewardHasNoStockToDecrement() {
        reward.setStock(null);
        stubHappyPath();

        service.redeem(new RedemptionRequestDto(10L, 5L));

        assertEquals(150, client.getPoints());
        verify(rewardRepository, never()).save(any());
    }

    @Test
    void redeemIsRefusedOutsideCatalogMode() {
        establishment.setRewardMode(RewardMode.DISCOUNT);
        when(clientRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(client));

        assertThrows(ConflictException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));

        assertEquals(200, client.getPoints());
    }

    @Test
    void redeemIsRefusedForInactiveOrOutOfStockRewards() {
        stubHappyPath();
        reward.setActive(false);
        assertThrows(ConflictException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));

        reward.setActive(true);
        reward.setStock(0);
        assertThrows(ConflictException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));

        assertEquals(200, client.getPoints());
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void redeemIsRefusedWhenThePointsAreNotEnough() {
        client.setPoints(49);
        stubHappyPath();

        assertThrows(InsufficientPointsException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));

        assertEquals(49, client.getPoints());
        verify(redemptionRepository, never()).save(any());
    }

    @Test
    void aRewardOfAnotherEstablishmentIsNotFound() {
        Establishment other = new Establishment();
        other.setId(2L);
        reward.setEstablishment(other);
        when(clientRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(client));
        when(rewardRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(reward));

        assertThrows(ResourceNotFoundException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));
        assertEquals(2, reward.getStock());
    }

    @Test
    void aClientOfAnotherEstablishmentIsForbiddenAndAdminGetsNotFound() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);
        when(clientRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(client));
        when(clientRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));
        assertThrows(ForbiddenException.class, () -> service.redeem(new RedemptionRequestDto(99L, 5L)));

        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        assertThrows(ResourceNotFoundException.class, () -> service.redeem(new RedemptionRequestDto(99L, 5L)));
    }

    @Test
    void adminCanRedeemForAnyClient() {
        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        stubHappyPath();

        assertNotNull(service.redeem(new RedemptionRequestDto(10L, 5L)).code());
    }

    @Test
    void redeemGivesUpWhenNoUniqueCodeCanBeFound() {
        stubHappyPath();
        when(redemptionRepository.existsByCode(any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.redeem(new RedemptionRequestDto(10L, 5L)));
    }

    @Test
    void redeemRetriesWhenACodeIsAlreadyTaken() {
        stubHappyPath();
        when(redemptionRepository.existsByCode(any())).thenReturn(true, false);

        assertNotNull(service.redeem(new RedemptionRequestDto(10L, 5L)).code());
    }

    @Test
    void listIsScopedToTheCallersEstablishmentAndAdminSeesAll() {
        Redemption redemption = Redemption.builder().id(1L).client(client).reward(reward).establishment(establishment)
                .pointsCost(50).code("ABCDEFGH").status(RedemptionStatus.ISSUED).build();
        when(redemptionRepository.search(1L, 10L)).thenReturn(List.of(redemption));
        when(redemptionRepository.search(null, null)).thenReturn(List.of(redemption, redemption));

        assertEquals(1, service.listRedemptions(10L).size());

        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        assertEquals(2, service.listRedemptions(null).size());
    }

    @Test
    void aVoucherCanBeUsedOnceOnlyInsideItsEstablishment() {
        Redemption redemption = Redemption.builder().id(1L).client(client).reward(reward).establishment(establishment)
                .pointsCost(50).code("ABCDEFGH").status(RedemptionStatus.ISSUED).build();
        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(redemption));
        when(redemptionRepository.save(redemption)).thenReturn(redemption);

        RedemptionDto used = service.markUsed(1L);

        assertEquals(RedemptionStatus.USED, used.status());
        assertNotNull(used.usedAt());
        assertThrows(ConflictException.class, () -> service.markUsed(1L));
    }

    @Test
    void usingAVoucherOfAnotherEstablishmentOrAMissingOneIsRejected() {
        Redemption redemption = Redemption.builder().id(1L).client(client).reward(reward).establishment(establishment)
                .pointsCost(50).code("ABCDEFGH").status(RedemptionStatus.ISSUED).build();
        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(redemption));
        when(redemptionRepository.findById(2L)).thenReturn(Optional.empty());

        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);
        assertThrows(ForbiddenException.class, () -> service.markUsed(1L));
        assertThrows(ForbiddenException.class, () -> service.markUsed(2L));

        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        assertThrows(ResourceNotFoundException.class, () -> service.markUsed(2L));
    }
}
