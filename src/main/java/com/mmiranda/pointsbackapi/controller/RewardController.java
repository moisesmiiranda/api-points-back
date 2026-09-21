package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.RewardDto;
import com.mmiranda.pointsbackapi.dto.RewardSettingsDto;
import com.mmiranda.pointsbackapi.service.RewardService;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RewardController {

    private static final String OWNER_OR_ADMIN = "hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')";

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping("/establishments/{id}/reward-settings")
    public RewardSettingsDto getSettings(@PathVariable Long id) {
        return rewardService.getSettings(id);
    }

    @PutMapping("/establishments/{id}/reward-settings")
    @PreAuthorize(OWNER_OR_ADMIN)
    public RewardSettingsDto updateSettings(@PathVariable Long id, @Valid @RequestBody RewardSettingsDto request) {
        return rewardService.updateSettings(id, request);
    }

    @PostMapping("/rewards")
    @PreAuthorize(OWNER_OR_ADMIN)
    public RewardDto createReward(@Validated(OnCreate.class) @RequestBody RewardDto request) {
        return rewardService.createReward(request);
    }

    @GetMapping("/rewards")
    public List<RewardDto> listRewards(@RequestParam(required = false) Long establishmentId) {
        return rewardService.listRewards(establishmentId);
    }

    @GetMapping("/rewards/{id}")
    public RewardDto getReward(@PathVariable Long id) {
        return rewardService.getReward(id);
    }

    @PutMapping("/rewards/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    public RewardDto updateReward(@PathVariable Long id, @Valid @RequestBody RewardDto request) {
        return rewardService.updateReward(id, request);
    }

    @DeleteMapping("/rewards/{id}")
    @PreAuthorize(OWNER_OR_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateReward(@PathVariable Long id) {
        rewardService.deactivateReward(id);
    }
}
