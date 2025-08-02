package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.EstablishmentDto;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstablishmentService {

    @Autowired
    private EstablishmentRepository establishmentRepository;

    public EstablishmentDto getEstablishmentById(Long id) {
        return establishmentRepository.findById(id)
                .map(EstablishmentDto::toDto)
                .orElse(null);
    }

    public Establishment createEstablishment(EstablishmentDto establishmentDto) {
        Establishment establishment = EstablishmentDto.toEntity(establishmentDto);
        return establishmentRepository.save(establishment);
    }

    public List<Establishment> listAllEstablishments() {
        return establishmentRepository.findAll();
    }
}

