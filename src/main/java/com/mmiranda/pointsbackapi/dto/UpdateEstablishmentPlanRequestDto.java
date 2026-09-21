package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.EstablishmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** {@code trialEndsAt} is only meaningful for TRIAL (when omitted it defaults to 14 days from today). */
public record UpdateEstablishmentPlanRequestDto(
        @NotNull EstablishmentStatus status,
        @Size(max = 100) String plan,
        LocalDate trialEndsAt
) {
}
