package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.LedgerEntryDto;
import com.mmiranda.pointsbackapi.dto.PageDto;
import com.mmiranda.pointsbackapi.exception.InsufficientPointsException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.LedgerEntry;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.model.Reward;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.LedgerRepository;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place that changes a client's points balance. Every change writes a ledger entry,
 * so {@code client.points} always equals the sum of the client's entries and can be audited.
 */
@Service
public class PointsService {

    static final int MAX_PAGE_SIZE = 100;

    private final ClientRepository clientRepository;
    private final LedgerRepository ledgerRepository;

    public PointsService(ClientRepository clientRepository, LedgerRepository ledgerRepository) {
        this.clientRepository = clientRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Applies a signed change to a balance and records it. The caller must already hold the client's
     * row lock ({@code ClientRepository#findByIdForUpdate}) inside a transaction, so the read-modify-write
     * of the balance cannot interleave with another change.
     *
     * @throws InsufficientPointsException if the balance would become negative
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry apply(Client lockedClient, PointsEntryType type, int delta,
                             Purchase purchase, Reward reward, String reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("A points change cannot be zero");
        }
        int current = lockedClient.getPoints() != null ? lockedClient.getPoints() : 0;
        int newBalance = current + delta;
        if (newBalance < 0) {
            throw new InsufficientPointsException(
                    "Insufficient points: balance is " + current + ", cannot remove " + (-delta));
        }
        lockedClient.setPoints(newBalance);
        clientRepository.save(lockedClient);

        return ledgerRepository.save(LedgerEntry.builder()
                .client(lockedClient)
                .type(type)
                .points(delta)
                .balanceAfter(newBalance)
                .purchase(purchase)
                .reward(reward)
                .reason(reason)
                .createdBy(SecurityUtils.getCurrentUser().userId())
                .build());
    }

    /** Newest first. The caller has already checked that the client is visible to the current user. */
    public PageDto<LedgerEntryDto> statement(Long clientId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<LedgerEntry> entries = ledgerRepository.findByClientIdOrderByIdDesc(
                clientId, PageRequest.of(safePage, safeSize));
        return new PageDto<>(entries.getContent().stream().map(LedgerEntryDto::toDto).toList(),
                entries.getNumber(), entries.getSize(), entries.getTotalElements(), entries.getTotalPages());
    }
}
