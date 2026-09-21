package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The commercial state of an establishment. {@code effectiveStatus} is what is enforced: an ended trial
 * counts as SUSPENDED even though {@code status} still says TRIAL. {@code trialDaysLeft} is only set
 * while the trial is running (0 on its last day).
 */
public record EstablishmentPlanDto(
        Long establishmentId,
        EstablishmentStatus status,
        EstablishmentStatus effectiveStatus,
        String plan,
        LocalDate trialEndsAt,
        Long trialDaysLeft
) {
    public static EstablishmentPlanDto toDto(Establishment establishment, LocalDate today) {
        EstablishmentStatus effective = establishment.effectiveStatus(today);
        boolean trialRunning = establishment.getStatus() == EstablishmentStatus.TRIAL
                && establishment.getTrialEndsAt() != null
                && effective == EstablishmentStatus.TRIAL;
        return new EstablishmentPlanDto(
                establishment.getId(),
                establishment.getStatus(),
                effective,
                establishment.getPlan(),
                establishment.getTrialEndsAt(),
                trialRunning ? ChronoUnit.DAYS.between(today, establishment.getTrialEndsAt()) : null);
    }
}
