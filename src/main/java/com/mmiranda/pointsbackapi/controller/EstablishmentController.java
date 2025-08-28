package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.EstablishmentDto;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.service.EstablishmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/establishments")
public class EstablishmentController {

    @Autowired
    private EstablishmentService establishmentService;

    @PostMapping
    public Establishment createEstablishment(@RequestBody EstablishmentDto establishmentDto) {
        return establishmentService.createEstablishment(establishmentDto);
    }

    @GetMapping("/all")
    public List<Establishment> listAllEstablishments() {
        return establishmentService.listAllEstablishments();
    }

    @GetMapping("/{id}")
    public EstablishmentDto getEstablishment(@PathVariable Long id) {
        return establishmentService.getEstablishmentById(id);
    }
}

