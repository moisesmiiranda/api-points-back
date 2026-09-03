package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByIdAndEstablishmentId(Long id, Long establishmentId);

    List<Client> findAllByEstablishmentId(Long establishmentId);
}
