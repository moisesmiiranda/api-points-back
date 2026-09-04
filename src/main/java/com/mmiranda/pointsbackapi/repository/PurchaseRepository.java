package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByPurchaseIdAndEstablishmentId(Long purchaseId, Long establishmentId);

    List<Purchase> findAllByEstablishmentId(Long establishmentId);
}

