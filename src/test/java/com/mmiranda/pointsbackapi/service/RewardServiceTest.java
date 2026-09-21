package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.RewardDto;
import com.mmiranda.pointsbackapi.dto.RewardSettingsDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Reward;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.model.RewardType;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.RewardRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private EstablishmentRepository establishmentRepository;

    @InjectMocks
    private RewardService service;

    private Establishment establishment;

    @BeforeEach
    void setUp() {
        establishment = new Establishment();
        establishment.setId(1L);
        TestAuth.asEstablishmentOwner(1L);
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    private Reward reward(Long id, Establishment owner) {
        return Reward.builder().id(id).establishment(owner).name("Coffee").type(RewardType.BRINDE)
                .pointsCost(50).active(true).stock(5).build();
    }

    // ------------------------------------------------------------------ settings

    @Test
    void readsAndUpdatesTheSettingsOfTheOwnEstablishment() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(establishmentRepository.save(establishment)).thenReturn(establishment);

        assertEquals(RewardMode.CATALOG, service.getSettings(1L).rewardMode());

        RewardSettingsDto updated = service.updateSettings(1L,
                new RewardSettingsDto(RewardMode.DISCOUNT, new BigDecimal("0.2500"), 40));
        assertEquals(RewardMode.DISCOUNT, updated.rewardMode());
        assertEquals(new BigDecimal("0.2500"), updated.pointsToCurrencyRate());
        assertEquals(40, updated.maxDiscountPercent());
    }

    @Test
    void omittedRateAndPercentageKeepTheirValues() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(establishmentRepository.save(establishment)).thenReturn(establishment);

        RewardSettingsDto updated = service.updateSettings(1L, new RewardSettingsDto(RewardMode.CASHBACK, null, null));

        assertEquals(RewardMode.CASHBACK, updated.rewardMode());
        assertEquals(new BigDecimal("0.1000"), updated.pointsToCurrencyRate());
        assertEquals(50, updated.maxDiscountPercent());
    }

    @Test
    void settingsOfAnotherEstablishmentAreOffLimits() {
        assertThrows(ForbiddenException.class, () -> service.getSettings(2L));
        assertThrows(ForbiddenException.class,
                () -> service.updateSettings(2L, new RewardSettingsDto(RewardMode.DISCOUNT, null, null)));
    }

    @Test
    void settingsOfAnUnknownEstablishmentAreNotFound() {
        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        when(establishmentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getSettings(9L));
    }

    // ------------------------------------------------------------------ catalog

    @Test
    void ownerCreatesARewardInTheirOwnEstablishmentAndItIsActiveByDefault() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(rewardRepository.save(any(Reward.class))).thenAnswer(i -> i.getArgument(0));

        RewardDto created = service.createReward(
                new RewardDto(null, null, "Coffee", "Free", RewardType.BRINDE, 50, null, null));

        assertEquals(1L, created.establishmentId());
        assertTrue(created.active());
        assertEquals("Coffee", created.name());
    }

    @Test
    void anInactiveRewardCanBeCreated() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(rewardRepository.save(any(Reward.class))).thenAnswer(i -> i.getArgument(0));

        assertFalse(service.createReward(
                new RewardDto(null, 1L, "Draft", null, RewardType.CUPOM, 10, false, 3)).active());
    }

    @Test
    void adminMustNameTheEstablishmentAndOthersCannotUseAnotherOne() {
        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        assertThrows(IllegalArgumentException.class, () -> service.createReward(
                new RewardDto(null, null, "x", null, RewardType.BRINDE, 1, null, null)));

        TestAuth.clear();
        TestAuth.asEstablishmentOwner(1L);
        assertThrows(ForbiddenException.class, () -> service.createReward(
                new RewardDto(null, 2L, "x", null, RewardType.BRINDE, 1, null, null)));
    }

    @Test
    void listsTheCatalogOfTheCallersEstablishmentAndAdminChoosesOne() {
        when(rewardRepository.findAllByEstablishmentIdOrderByIdAsc(1L)).thenReturn(List.of(reward(1L, establishment)));

        assertEquals(1, service.listRewards(99L).size()); // the requested id is ignored for non-admins

        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        assertThrows(IllegalArgumentException.class, () -> service.listRewards(null));
        assertEquals(1, service.listRewards(1L).size());
    }

    @Test
    void getsARewardOnlyInsideTheOwnEstablishment() {
        Establishment other = new Establishment();
        other.setId(2L);
        when(rewardRepository.findById(1L)).thenReturn(Optional.of(reward(1L, establishment)));
        when(rewardRepository.findById(2L)).thenReturn(Optional.of(reward(2L, other)));
        when(rewardRepository.findById(3L)).thenReturn(Optional.empty());

        assertEquals(1L, service.getReward(1L).id());
        assertThrows(ForbiddenException.class, () -> service.getReward(2L));
        assertThrows(ForbiddenException.class, () -> service.getReward(3L)); // same answer as "not yours"

        TestAuth.clear();
        TestAuth.asPlatformAdmin();
        assertEquals(2L, service.getReward(2L).id());
        assertThrows(ResourceNotFoundException.class, () -> service.getReward(3L));
    }

    @Test
    void updatesOnlyTheFieldsThatWereSent() {
        Reward existing = reward(1L, establishment);
        when(rewardRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(rewardRepository.save(existing)).thenReturn(existing);

        service.updateReward(1L, new RewardDto(null, null, null, null, null, null, null, null));
        assertEquals("Coffee", existing.getName());
        assertEquals(50, existing.getPointsCost());

        RewardDto updated = service.updateReward(1L,
                new RewardDto(null, null, "Tea", "Hot", RewardType.CUPOM, 70, false, 0));
        assertEquals("Tea", updated.name());
        assertEquals("Hot", updated.description());
        assertEquals(RewardType.CUPOM, updated.type());
        assertEquals(70, updated.pointsCost());
        assertFalse(updated.active());
        assertEquals(0, updated.stock());
    }

    @Test
    void deactivatingKeepsTheRewardForHistory() {
        Reward existing = reward(1L, establishment);
        when(rewardRepository.findById(1L)).thenReturn(Optional.of(existing));

        service.deactivateReward(1L);

        assertFalse(existing.isActive());
    }
}
