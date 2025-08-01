package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
