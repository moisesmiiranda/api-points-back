package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Purchase;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.repository.PurchaseRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PurchaseService {
    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EstablishmentRepository establishmentRepository;

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
        Purchase purchase = requireScopedPurchase(id);
        return purchase != null ? PurchaseDto.toDto(purchase) : null;
    }

    public void registerPurchase(PurchaseDto purchaseDto) {
        Establishment establishment = resolveEstablishmentForWrite(purchaseDto.establishmentId());

        Client client = clientRepository.findById(purchaseDto.clientId())
                .filter(c -> c.getEstablishment() != null
                        && c.getEstablishment().getId().equals(establishment.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find client with id: " + purchaseDto.clientId() + " in this establishment"));

        Purchase purchase = new Purchase();
        purchase.setClient(client);
        purchase.setEstablishment(establishment);
        purchase.setAmount(purchaseDto.amount());

        int pointsEarned = calculatePoints(establishment.getValuePerPoint(), purchaseDto.amount());

        client.setPoints(client.getPoints() + pointsEarned);

        clientRepository.save(client);
        purchaseRepository.save(purchase);
    }

    private int calculatePoints(Integer valuePerPoints, BigDecimal purchaseValue) {
        return purchaseValue.divide(BigDecimal.valueOf(valuePerPoints), RoundingMode.HALF_DOWN).intValue();
    }

    public PurchaseDto updatePurchaseById(Long purchaseId, PurchaseDto purchaseDto) {
        Purchase purchaseEntity = requireScopedPurchase(purchaseId);
        if (purchaseEntity == null) {
            return null;
        }

        // Update only non-null fields
        if (purchaseDto.clientId() != null) {
            var client = clientRepository.findById(purchaseDto.clientId());
            if (client.isEmpty()) {
                return null;
            }
            purchaseEntity.setClient(client.get());
        }

        if (purchaseDto.establishmentId() != null) {
            SecurityUtils.requireEstablishmentAccess(purchaseDto.establishmentId());
            var establishment = establishmentRepository.findById(purchaseDto.establishmentId());
            if (establishment.isEmpty()) {
                return null;
            }
            purchaseEntity.setEstablishment(establishment.get());
        }

        if (purchaseDto.amount() != null) {
            purchaseEntity.setAmount(purchaseDto.amount());
        }

        Purchase updatedPurchase = purchaseRepository.save(purchaseEntity);
        return PurchaseDto.toDto(updatedPurchase);
    }

    /**
     * PLATFORM_ADMIN sees a plain not-found (null) for a missing id. Every other role gets
     * an identical ForbiddenException whether the id belongs to another establishment or
     * doesn't exist at all, so a 403 never leaks which case it was.
     */
    private Purchase requireScopedPurchase(long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        if (caller.isPlatformAdmin()) {
            return purchaseRepository.findById(id).orElse(null);
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
