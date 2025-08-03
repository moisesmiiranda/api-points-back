package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByEstablishmentId(Long establishmentId);
}

