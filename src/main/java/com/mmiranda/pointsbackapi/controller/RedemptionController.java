package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.RedemptionDto;
import com.mmiranda.pointsbackapi.dto.RedemptionRequestDto;
import com.mmiranda.pointsbackapi.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/redemptions")
public class RedemptionController {

    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping
    public RedemptionDto redeem(@Valid @RequestBody RedemptionRequestDto request) {
        return redemptionService.redeem(request);
    }

    @GetMapping
    public List<RedemptionDto> listRedemptions(@RequestParam(required = false) Long clientId) {
        return redemptionService.listRedemptions(clientId);
    }

    @PostMapping("/{id}/use")
    public RedemptionDto markUsed(@PathVariable Long id) {
        return redemptionService.markUsed(id);
    }
}
