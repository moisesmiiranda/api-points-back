package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/establishments")
public class EstablishmentController {

    @Autowired
    private EstablishmentRepository establishmentRepository;

    @PostMapping
    public Establishment createEstablishment(Establishment establishment) {
        return establishmentRepository.save(establishment);
    }

    @GetMapping("/list")
    public List<Establishment> listAllEstablishments() {
        return establishmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Establishment getEstablishment(@PathVariable Long id) {
        return establishmentRepository.findById(id).orElse(null);
    }
}
