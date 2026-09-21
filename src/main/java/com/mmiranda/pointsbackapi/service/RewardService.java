package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.RewardDto;
import com.mmiranda.pointsbackapi.dto.RewardSettingsDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Reward;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.RewardRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * How an establishment lets clients spend points (its reward mode and conversion settings) and its
 * catalog of gifts and coupons. Writes are restricted to owners and admins by the controller.
 */
@Service
public class RewardService {

    private final RewardRepository rewardRepository;
    private final EstablishmentRepository establishmentRepository;

    public RewardService(RewardRepository rewardRepository, EstablishmentRepository establishmentRepository) {
        this.rewardRepository = rewardRepository;
        this.establishmentRepository = establishmentRepository;
    }

    // ------------------------------------------------------------------ settings

    public RewardSettingsDto getSettings(Long establishmentId) {
        SecurityUtils.requireEstablishmentAccess(establishmentId);
        return RewardSettingsDto.toDto(requireEstablishment(establishmentId));
    }

    @Transactional
    public RewardSettingsDto updateSettings(Long establishmentId, RewardSettingsDto request) {
        SecurityUtils.requireEstablishmentAccess(establishmentId);
        Establishment establishment = requireEstablishment(establishmentId);
        establishment.setRewardMode(request.rewardMode());
        if (request.pointsToCurrencyRate() != null) {
            establishment.setPointsToCurrencyRate(request.pointsToCurrencyRate());
        }
        if (request.maxDiscountPercent() != null) {
            establishment.setMaxDiscountPercent(request.maxDiscountPercent());
        }
        return RewardSettingsDto.toDto(establishmentRepository.save(establishment));
    }

    // ------------------------------------------------------------------ catalog

    @Transactional
    public RewardDto createReward(RewardDto request) {
        Establishment establishment = resolveEstablishmentForWrite(request.establishmentId());
        Reward reward = Reward.builder()
                .establishment(establishment)
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .pointsCost(request.pointsCost())
                .active(request.active() == null || request.active())
                .stock(request.stock())
                .build();
        return RewardDto.toDto(rewardRepository.save(reward));
    }

    /** A caller sees its own establishment's catalog; PLATFORM_ADMIN must say which establishment. */
    public List<RewardDto> listRewards(Long establishmentId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Long scope = caller.isPlatformAdmin() ? establishmentId : caller.establishmentId();
        if (scope == null) {
            throw new IllegalArgumentException("establishmentId is required");
        }
        return rewardRepository.findAllByEstablishmentIdOrderByIdAsc(scope).stream()
                .map(RewardDto::toDto)
                .toList();
    }

    public RewardDto getReward(Long id) {
        return RewardDto.toDto(requireScopedReward(id));
    }

    @Transactional
    public RewardDto updateReward(Long id, RewardDto request) {
        Reward reward = requireScopedReward(id);
        if (request.name() != null) {
            reward.setName(request.name());
        }
        if (request.description() != null) {
            reward.setDescription(request.description());
        }
        if (request.type() != null) {
            reward.setType(request.type());
        }
        if (request.pointsCost() != null) {
            reward.setPointsCost(request.pointsCost());
        }
        if (request.active() != null) {
            reward.setActive(request.active());
        }
        if (request.stock() != null) {
            reward.setStock(request.stock());
        }
        return RewardDto.toDto(rewardRepository.save(reward));
    }

    /** Rewards are never deleted (redemptions and the ledger refer to them): they are deactivated. */
    @Transactional
    public void deactivateReward(Long id) {
        Reward reward = requireScopedReward(id);
        reward.setActive(false);
        rewardRepository.save(reward);
    }

    private Reward requireScopedReward(Long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Reward reward = rewardRepository.findById(id).orElse(null);
        if (caller.isPlatformAdmin()) {
            if (reward == null) {
                throw new ResourceNotFoundException("Cannot find reward with id: " + id);
            }
            return reward;
        }
        if (reward == null || !caller.belongsToEstablishment(reward.getEstablishment().getId())) {
            throw new ForbiddenException("You do not have access to this reward");
        }
        return reward;
    }

    private Establishment resolveEstablishmentForWrite(Long requestedEstablishmentId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Long establishmentId = requestedEstablishmentId;
        if (caller.isPlatformAdmin()) {
            if (establishmentId == null) {
                throw new IllegalArgumentException("establishmentId is required");
            }
        } else if (establishmentId == null) {
            establishmentId = caller.establishmentId();
        } else if (!caller.belongsToEstablishment(establishmentId)) {
            throw new ForbiddenException("You do not have access to this establishment");
        }
        return requireEstablishment(establishmentId);
    }

    private Establishment requireEstablishment(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find establishment with id: " + id));
    }
}
