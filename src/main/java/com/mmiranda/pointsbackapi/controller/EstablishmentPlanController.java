package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.EstablishmentPlanDto;
import com.mmiranda.pointsbackapi.dto.UpdateEstablishmentPlanRequestDto;
import com.mmiranda.pointsbackapi.service.EstablishmentPlanService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstablishmentPlanController {

    private final EstablishmentPlanService planService;

    public EstablishmentPlanController(EstablishmentPlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/establishments/{id}/plan")
    public EstablishmentPlanDto getPlan(@PathVariable Long id) {
        return planService.getPlan(id);
    }

    @PutMapping("/establishments/{id}/plan")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public EstablishmentPlanDto updatePlan(@PathVariable Long id,
                                           @Valid @RequestBody UpdateEstablishmentPlanRequestDto request) {
        return planService.updatePlan(id, request);
    }
}
