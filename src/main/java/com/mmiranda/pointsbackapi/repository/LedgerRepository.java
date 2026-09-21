package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    Page<LedgerEntry> findByClientIdOrderByIdDesc(Long clientId, Pageable pageable);
}
