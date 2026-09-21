package com.mmiranda.pointsbackapi.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstablishmentStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);

    private Establishment establishment(EstablishmentStatus status, LocalDate trialEndsAt) {
        Establishment establishment = new Establishment();
        establishment.setStatus(status);
        establishment.setTrialEndsAt(trialEndsAt);
        return establishment;
    }

    @Test
    void aRunningTrialIsTrialUntilItsLastDayInclusive() {
        assertEquals(EstablishmentStatus.TRIAL, establishment(EstablishmentStatus.TRIAL, TODAY).effectiveStatus(TODAY));
        assertEquals(EstablishmentStatus.TRIAL, establishment(EstablishmentStatus.TRIAL, TODAY.plusDays(3)).effectiveStatus(TODAY));
        assertFalse(establishment(EstablishmentStatus.TRIAL, TODAY).isBlocked(TODAY));
    }

    @Test
    void anEndedTrialBehavesAsSuspended() {
        Establishment ended = establishment(EstablishmentStatus.TRIAL, TODAY.minusDays(1));

        assertEquals(EstablishmentStatus.SUSPENDED, ended.effectiveStatus(TODAY));
        assertEquals(EstablishmentStatus.TRIAL, ended.getStatus());
        assertTrue(ended.isBlocked(TODAY));
    }

    @Test
    void aTrialWithoutAnEndDateNeverExpires() {
        assertFalse(establishment(EstablishmentStatus.TRIAL, null).isBlocked(TODAY));
    }

    @Test
    void activeIsNeverBlockedEvenWithAnOldTrialDateAndSuspendedAlwaysIs() {
        assertFalse(establishment(EstablishmentStatus.ACTIVE, TODAY.minusDays(90)).isBlocked(TODAY));
        assertTrue(establishment(EstablishmentStatus.SUSPENDED, null).isBlocked(TODAY));
        assertTrue(establishment(EstablishmentStatus.SUSPENDED, TODAY.plusDays(30)).isBlocked(TODAY));
    }

    @Test
    void aNewEstablishmentStartsAFourteenDayTrial() {
        Establishment fresh = new Establishment();
        assertEquals(EstablishmentStatus.TRIAL, fresh.getStatus());

        fresh.applyTrialDefaults();

        assertEquals(LocalDate.now().plusDays(Establishment.DEFAULT_TRIAL_DAYS), fresh.getTrialEndsAt());
    }

    @Test
    void trialDefaultsDoNotOverwriteAChosenDateOrTouchOtherStatuses() {
        Establishment chosen = establishment(EstablishmentStatus.TRIAL, TODAY.plusDays(5));
        chosen.applyTrialDefaults();
        assertEquals(TODAY.plusDays(5), chosen.getTrialEndsAt());

        Establishment active = establishment(EstablishmentStatus.ACTIVE, null);
        active.applyTrialDefaults();
        assertEquals(null, active.getTrialEndsAt());
    }
}
