package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.RedemptionDto;
import com.mmiranda.pointsbackapi.dto.RedemptionRequestDto;
import com.mmiranda.pointsbackapi.exception.ConflictException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.Redemption;
import com.mmiranda.pointsbackapi.model.RedemptionStatus;
import com.mmiranda.pointsbackapi.model.Reward;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.RedemptionRepository;
import com.mmiranda.pointsbackapi.repository.RewardRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/** Exchanging points for a catalog reward (CATALOG mode) and handing the voucher over. */
@Service
public class RedemptionService {

    /** No 0/O/1/I so a code read out loud or copied by hand is not misread. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODE_ATTEMPTS = 5;

    private final ClientRepository clientRepository;
    private final RewardRepository rewardRepository;
    private final RedemptionRepository redemptionRepository;
    private final PointsService pointsService;
    private final SecureRandom random = new SecureRandom();

    public RedemptionService(ClientRepository clientRepository, RewardRepository rewardRepository,
                              RedemptionRepository redemptionRepository, PointsService pointsService) {
        this.clientRepository = clientRepository;
        this.rewardRepository = rewardRepository;
        this.redemptionRepository = redemptionRepository;
        this.pointsService = pointsService;
    }

    /**
     * Charges the reward's cost to the client's balance, takes one unit from a limited stock and issues
     * a voucher. Only allowed when the establishment's mode is CATALOG. The client and the reward are
     * locked, so concurrent redemptions can neither overspend a balance nor oversell the stock.
     */
    @Transactional
    public RedemptionDto redeem(RedemptionRequestDto request) {
        Client client = lockScopedClient(request.clientId());
        if (client.getEstablishment().getRewardMode() != RewardMode.CATALOG) {
            throw new ConflictException("This establishment does not exchange points for catalog rewards");
        }

        Reward reward = rewardRepository.findByIdForUpdate(request.rewardId())
                .filter(r -> r.getEstablishment().getId().equals(client.getEstablishment().getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find reward with id: " + request.rewardId() + " in this establishment"));
        if (!reward.isActive()) {
            throw new ConflictException("This reward is not available");
        }
        if (reward.getStock() != null) {
            if (reward.getStock() <= 0) {
                throw new ConflictException("This reward is out of stock");
            }
            reward.setStock(reward.getStock() - 1);
            rewardRepository.save(reward);
        }

        pointsService.apply(client, PointsEntryType.REDEEM, -reward.getPointsCost(), null, reward,
                "Reward: " + reward.getName());

        Redemption redemption = Redemption.builder()
                .client(client)
                .reward(reward)
                .establishment(client.getEstablishment())
                .pointsCost(reward.getPointsCost())
                .code(newCode())
                .status(RedemptionStatus.ISSUED)
                .createdBy(SecurityUtils.getCurrentUser().userId())
                .build();
        return RedemptionDto.toDto(redemptionRepository.save(redemption));
    }

    /** Newest first. Non-admin callers only see their own establishment; a client filter is optional. */
    public List<RedemptionDto> listRedemptions(Long clientId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Long scope = caller.isPlatformAdmin() ? null : caller.establishmentId();
        return redemptionRepository.search(scope, clientId).stream().map(RedemptionDto::toDto).toList();
    }

    /** Marks a voucher as handed over, once. */
    @Transactional
    public RedemptionDto markUsed(Long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Redemption redemption = redemptionRepository.findById(id).orElse(null);
        if (redemption == null || (!caller.isPlatformAdmin()
                && !caller.belongsToEstablishment(redemption.getEstablishment().getId()))) {
            // Admins learn a redemption does not exist; everyone else gets the same answer for "missing" and "not yours"
            if (caller.isPlatformAdmin()) {
                throw new ResourceNotFoundException("Cannot find redemption with id: " + id);
            }
            throw new ForbiddenException("You do not have access to this redemption");
        }
        if (redemption.getStatus() == RedemptionStatus.USED) {
            throw new ConflictException("This voucher was already used");
        }
        redemption.setStatus(RedemptionStatus.USED);
        redemption.setUsedAt(LocalDateTime.now());
        return RedemptionDto.toDto(redemptionRepository.save(redemption));
    }

    private Client lockScopedClient(Long clientId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Client client = clientRepository.findByIdForUpdate(clientId).orElse(null);
        if (caller.isPlatformAdmin()) {
            if (client == null) {
                throw new ResourceNotFoundException("Cannot find client with id: " + clientId);
            }
            return client;
        }
        if (client == null || client.getEstablishment() == null
                || !caller.belongsToEstablishment(client.getEstablishment().getId())) {
            throw new ForbiddenException("You do not have access to this client");
        }
        return client;
    }

    private String newCode() {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            if (!redemptionRepository.existsByCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Could not generate a unique voucher code");
    }
}
