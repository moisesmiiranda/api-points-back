package com.mmiranda.pointsbackapi.repository;

import com.mmiranda.pointsbackapi.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByCpf(String cpf);
}
