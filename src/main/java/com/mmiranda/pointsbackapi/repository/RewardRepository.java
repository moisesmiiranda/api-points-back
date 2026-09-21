package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Reward;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findAllByEstablishmentIdOrderByIdAsc(Long establishmentId);

    /** Row lock, so concurrent redemptions cannot oversell a limited stock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reward r where r.id = :id")
    Optional<Reward> findByIdForUpdate(@Param("id") Long id);
}
