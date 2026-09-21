package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.EstablishmentPlanDto;
import com.mmiranda.pointsbackapi.dto.UpdateEstablishmentPlanRequestDto;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentStatus;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** The commercial state of an establishment (trial, active, suspended). Changing it is restricted to PLATFORM_ADMIN. */
@Service
public class EstablishmentPlanService {

    private final EstablishmentRepository establishmentRepository;
    private final Clock clock;

    public EstablishmentPlanService(EstablishmentRepository establishmentRepository, Clock clock) {
        this.establishmentRepository = establishmentRepository;
        this.clock = clock;
    }

    public EstablishmentPlanDto getPlan(Long establishmentId) {
        SecurityUtils.requireEstablishmentAccess(establishmentId);
        return EstablishmentPlanDto.toDto(require(establishmentId), LocalDate.now(clock));
    }

    @Transactional
    public EstablishmentPlanDto updatePlan(Long establishmentId, UpdateEstablishmentPlanRequestDto request) {
        LocalDate today = LocalDate.now(clock);
        Establishment establishment = require(establishmentId);

        establishment.setStatus(request.status());
        establishment.setPlan(request.plan());
        if (request.status() == EstablishmentStatus.TRIAL) {
            LocalDate current = establishment.getTrialEndsAt();
            LocalDate keep = current != null && !current.isBefore(today) ? current : today.plusDays(Establishment.DEFAULT_TRIAL_DAYS);
            establishment.setTrialEndsAt(request.trialEndsAt() != null ? request.trialEndsAt() : keep);
        } else if (request.trialEndsAt() != null) {
            establishment.setTrialEndsAt(request.trialEndsAt());
        }
        return EstablishmentPlanDto.toDto(establishmentRepository.save(establishment), today);
    }

    private Establishment require(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find establishment with id: " + id));
    }
}
