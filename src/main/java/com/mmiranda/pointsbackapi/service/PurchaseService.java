package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.exception.ConflictException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.PurchaseRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class PurchaseService {
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EstablishmentRepository establishmentRepository;
    @Autowired
    private PointsService pointsService;

    public List<PurchaseDto> listAllPurchases() {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        List<Purchase> purchases = caller.isPlatformAdmin()
                ? purchaseRepository.findAll()
                : purchaseRepository.findAllByEstablishmentId(caller.establishmentId());
        return purchases.stream()
                .map(PurchaseDto::toDto)
                .toList();
    }

    public PurchaseDto getPurchaseById(long id) {
        return PurchaseDto.toDto(requireScopedPurchase(id));
    }

    /**
     * Registers a sale. When {@code redeemPoints} is given, those points pay part of it (only in the
     * DISCOUNT and CASHBACK modes, and never above the establishment's maximum share); points are then
     * earned on what was actually paid.
     */
    @Transactional
    public PurchaseDto registerPurchase(PurchaseDto purchaseDto) {
        Establishment establishment = resolveEstablishmentForWrite(purchaseDto.establishmentId());

        Client client = clientRepository.findByIdForUpdate(purchaseDto.clientId())
                .filter(c -> belongsTo(c, establishment))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find client with id: " + purchaseDto.clientId() + " in this establishment"));

        int redeemPoints = purchaseDto.redeemPoints() != null ? purchaseDto.redeemPoints() : 0;
        BigDecimal discount = discountFor(establishment, purchaseDto.amount(), redeemPoints);
        int pointsEarned = RewardCalculator.pointsFor(establishment.getValuePerPoint(),
                purchaseDto.amount().subtract(discount));

        Purchase purchase = new Purchase();
        purchase.setClient(client);
        purchase.setEstablishment(establishment);
        purchase.setAmount(purchaseDto.amount());
        purchase.setDiscountAmount(discount);
        purchase.setPointsRedeemed(redeemPoints);
        purchase.setPointsEarned(pointsEarned);
        Purchase saved = purchaseRepository.save(purchase);

        if (redeemPoints > 0) {
            pointsService.apply(client, PointsEntryType.REDEEM, -redeemPoints, saved, null, "Discount on purchase");
        }
        if (pointsEarned > 0) {
            pointsService.apply(client, PointsEntryType.CREDIT, pointsEarned, saved, null, null);
        }
        return PurchaseDto.toDto(saved);
    }

    /**
     * Updates a purchase and keeps the clients' balances consistent with it: the points the old
     * purchase granted are taken back and the points of the new values are granted, each as a ledger
     * entry. The target client must belong to the purchase's establishment. A purchase that was paid
     * partly with points, or that is cancelled, cannot be edited (cancel it and register it again).
     * Affected clients are locked in id order so concurrent updates cannot deadlock.
     */
    @Transactional
    public PurchaseDto updatePurchaseById(Long purchaseId, PurchaseDto purchaseDto) {
        Purchase purchase = requireScopedPurchase(purchaseId);
        if (purchase.getCancelledAt() != null) {
            throw new ConflictException("A cancelled purchase cannot be edited");
        }
        if (purchase.getPointsRedeemed() > 0) {
            throw new ConflictException(
                    "A purchase paid with points cannot be edited; cancel it and register it again");
        }

        Long oldClientId = purchase.getClient().getId();
        int oldPoints = purchase.getPointsEarned();

        Establishment newEstablishment = purchase.getEstablishment();
        if (purchaseDto.establishmentId() != null) {
            SecurityUtils.requireEstablishmentAccess(purchaseDto.establishmentId());
            newEstablishment = establishmentRepository.findById(purchaseDto.establishmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cannot find establishment with id: " + purchaseDto.establishmentId()));
        }
        Long newClientId = purchaseDto.clientId() != null ? purchaseDto.clientId() : oldClientId;
        BigDecimal newAmount = purchaseDto.amount() != null ? purchaseDto.amount() : purchase.getAmount();
        int newPoints = RewardCalculator.pointsFor(newEstablishment.getValuePerPoint(), newAmount);

        Map<Long, Client> lockedClients = new HashMap<>();
        for (Long clientId : new TreeSet<>(List.of(oldClientId, newClientId))) {
            lockedClients.put(clientId, clientRepository.findByIdForUpdate(clientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cannot find client with id: " + clientId)));
        }
        Client oldClient = lockedClients.get(oldClientId);
        Client newClient = lockedClients.get(newClientId);

        if (!belongsTo(newClient, newEstablishment)) {
            throw new ResourceNotFoundException(
                    "Cannot find client with id: " + newClientId + " in this establishment");
        }

        boolean sameClient = oldClientId.equals(newClientId);
        if (!sameClient || oldPoints != newPoints) {
            // Credit first: when the client is the same the two entries net out, so a client who already
            // spent part of the old points can still have a purchase corrected upwards.
            if (newPoints > 0) {
                pointsService.apply(newClient, PointsEntryType.CREDIT, newPoints, purchase, null,
                        "Purchase corrected");
            }
            if (oldPoints > 0) {
                pointsService.apply(oldClient, PointsEntryType.REVERSAL, -oldPoints, purchase, null,
                        "Purchase corrected");
            }
        }

        purchase.setClient(newClient);
        purchase.setEstablishment(newEstablishment);
        purchase.setAmount(newAmount);
        purchase.setPointsEarned(newPoints);

        return PurchaseDto.toDto(purchaseRepository.save(purchase));
    }

    /**
     * Cancels a purchase: points spent on it are returned and points it granted are taken back, as
     * REVERSAL entries. Refused (422) if the client has already spent the points the purchase granted.
     */
    @Transactional
    public PurchaseDto cancelPurchase(Long purchaseId) {
        Purchase purchase = requireScopedPurchase(purchaseId);
        if (purchase.getCancelledAt() != null) {
            throw new ConflictException("This purchase is already cancelled");
        }

        Client client = clientRepository.findByIdForUpdate(purchase.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find client with id: " + purchase.getClient().getId()));

        // Return the redeemed points first so that taking back the earned ones is judged on the restored balance
        if (purchase.getPointsRedeemed() > 0) {
            pointsService.apply(client, PointsEntryType.REVERSAL, purchase.getPointsRedeemed(), purchase, null,
                    "Purchase cancelled: points returned");
        }
        if (purchase.getPointsEarned() > 0) {
            pointsService.apply(client, PointsEntryType.REVERSAL, -purchase.getPointsEarned(), purchase, null,
                    "Purchase cancelled: points removed");
        }
        purchase.setCancelledAt(LocalDateTime.now());
        return PurchaseDto.toDto(purchaseRepository.save(purchase));
    }

    private BigDecimal discountFor(Establishment establishment, BigDecimal amount, int redeemPoints) {
        if (redeemPoints == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (establishment.getRewardMode() == RewardMode.CATALOG) {
            throw new ConflictException("This establishment does not offer discounts with points");
        }
        BigDecimal discount = RewardCalculator.discountFor(establishment, redeemPoints);
        BigDecimal maxDiscount = RewardCalculator.maxDiscount(establishment, amount);
        if (discount.compareTo(maxDiscount) > 0) {
            throw new IllegalArgumentException("The discount of " + discount + " exceeds the maximum of "
                    + maxDiscount + " (" + establishment.getMaxDiscountPercent() + "% of the purchase)");
        }
        return discount;
    }

    private static boolean belongsTo(Client client, Establishment establishment) {
        return client.getEstablishment() != null
                && client.getEstablishment().getId().equals(establishment.getId());
    }

    /**
     * PLATFORM_ADMIN gets a plain 404 for a missing id. Every other role gets an identical
     * ForbiddenException whether the id belongs to another establishment or doesn't exist at
     * all, so a 403 never leaks which case it was.
     */
    private Purchase requireScopedPurchase(long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        if (caller.isPlatformAdmin()) {
            return purchaseRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Cannot find purchase with id: " + id));
        }
        return purchaseRepository.findByPurchaseIdAndEstablishmentId(id, caller.establishmentId())
                .orElseThrow(() -> new ForbiddenException("You do not have access to this purchase"));
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

        Long resolvedEstablishmentId = establishmentId;
        return establishmentRepository.findById(resolvedEstablishmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find establishment with id: " + resolvedEstablishmentId));
    }
}
