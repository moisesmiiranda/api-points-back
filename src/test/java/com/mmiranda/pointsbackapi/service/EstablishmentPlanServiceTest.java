package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.EstablishmentPlanDto;
import com.mmiranda.pointsbackapi.dto.UpdateEstablishmentPlanRequestDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentStatus;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablishmentPlanServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);

    @Mock
    private EstablishmentRepository repository;

    private EstablishmentPlanService service;
    private Establishment establishment;

    @BeforeEach
    void setUp() {
        service = new EstablishmentPlanService(repository, Clock.fixed(Instant.parse("2026-09-20T15:00:00Z"), ZoneOffset.UTC));
        establishment = new Establishment();
        establishment.setId(1L);
        TestAuth.asPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    private void stubFind() {
        when(repository.findById(1L)).thenReturn(Optional.of(establishment));
        when(repository.save(any(Establishment.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void showsStatusEffectiveStatusAndTheDaysLeftOfARunningTrial() {
        establishment.setStatus(EstablishmentStatus.TRIAL);
        establishment.setTrialEndsAt(TODAY.plusDays(5));
        establishment.setPlan("Básico");
        when(repository.findById(1L)).thenReturn(Optional.of(establishment));

        EstablishmentPlanDto plan = service.getPlan(1L);

        assertEquals(EstablishmentStatus.TRIAL, plan.status());
        assertEquals(EstablishmentStatus.TRIAL, plan.effectiveStatus());
        assertEquals(5L, plan.trialDaysLeft());
        assertEquals("Básico", plan.plan());
    }

    @Test
    void anEndedTrialShowsAsSuspendedWithoutDaysLeft() {
        establishment.setStatus(EstablishmentStatus.TRIAL);
        establishment.setTrialEndsAt(TODAY.minusDays(2));
        when(repository.findById(1L)).thenReturn(Optional.of(establishment));

        EstablishmentPlanDto plan = service.getPlan(1L);

        assertEquals(EstablishmentStatus.TRIAL, plan.status());
        assertEquals(EstablishmentStatus.SUSPENDED, plan.effectiveStatus());
        assertNull(plan.trialDaysLeft());
    }

    @Test
    void activeHasNoTrialCountdown() {
        establishment.setStatus(EstablishmentStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(establishment));

        assertNull(service.getPlan(1L).trialDaysLeft());
    }

    @Test
    void ownersSeeOnlyTheirOwnPlan() {
        TestAuth.clear();
        TestAuth.asEstablishmentOwner(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(establishment));

        assertEquals(1L, service.getPlan(1L).establishmentId());
        assertThrows(ForbiddenException.class, () -> service.getPlan(2L));
    }

    @Test
    void anUnknownEstablishmentIsNotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getPlan(1L));
        assertThrows(ResourceNotFoundException.class,
                () -> service.updatePlan(1L, new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.ACTIVE, null, null)));
    }

    @Test
    void suspendingKeepsTheTrialDateAndStoresThePlanLabel() {
        establishment.setStatus(EstablishmentStatus.TRIAL);
        establishment.setTrialEndsAt(TODAY.plusDays(4));
        stubFind();

        EstablishmentPlanDto plan = service.updatePlan(1L, new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.SUSPENDED, "Básico", null));

        assertEquals(EstablishmentStatus.SUSPENDED, plan.status());
        assertEquals(EstablishmentStatus.SUSPENDED, plan.effectiveStatus());
        assertEquals(TODAY.plusDays(4), plan.trialEndsAt());
        assertEquals("Básico", plan.plan());
    }

    @Test
    void activatingSetsActiveAndAcceptsANewDateOnlyIfGiven() {
        establishment.setStatus(EstablishmentStatus.SUSPENDED);
        stubFind();

        EstablishmentPlanDto plan = service.updatePlan(1L, new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.ACTIVE, "Pro", null));

        assertEquals(EstablishmentStatus.ACTIVE, plan.effectiveStatus());
        assertNull(plan.trialEndsAt());
        assertEquals(TODAY.plusDays(60), service.updatePlan(1L,
                new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.ACTIVE, "Pro", TODAY.plusDays(60))).trialEndsAt());
    }

    @Test
    void startingATrialWithoutADateGivesFourteenDaysOrKeepsARunningOne() {
        establishment.setStatus(EstablishmentStatus.SUSPENDED);
        establishment.setTrialEndsAt(TODAY.minusDays(30)); // an old, ended trial
        stubFind();

        assertEquals(TODAY.plusDays(14), service.updatePlan(1L,
                new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.TRIAL, null, null)).trialEndsAt());

        establishment.setTrialEndsAt(TODAY.plusDays(3)); // still running: keep it
        assertEquals(TODAY.plusDays(3), service.updatePlan(1L,
                new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.TRIAL, null, null)).trialEndsAt());
    }

    @Test
    void anExplicitTrialDateWins() {
        stubFind();

        EstablishmentPlanDto plan = service.updatePlan(1L,
                new UpdateEstablishmentPlanRequestDto(EstablishmentStatus.TRIAL, null, TODAY.minusDays(1)));

        assertEquals(TODAY.minusDays(1), plan.trialEndsAt());
        assertEquals(EstablishmentStatus.SUSPENDED, plan.effectiveStatus());
    }
}
