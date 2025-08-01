package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {
}
