package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
}

