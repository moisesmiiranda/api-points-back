package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    boolean existsByCode(String code);

    /** Redemptions of one establishment (or of all when null), optionally of one client, newest first. */
    @Query("""
            select r from Redemption r
            where (:establishmentId is null or r.establishment.id = :establishmentId)
              and (:clientId is null or r.client.id = :clientId)
            order by r.id desc
            """)
    List<Redemption> search(@Param("establishmentId") Long establishmentId, @Param("clientId") Long clientId);
}
