package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByIdAsc();

    List<User> findAllByEstablishmentIdAndRoleOrderByIdAsc(Long establishmentId, Role role);
}
