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

    @SuppressWarnings("null")
    public EstablishmentDto updateEstablishmentById(Long establishmentId, EstablishmentDto establishmentDto) {
        var establishment = establishmentRepository.findById(establishmentId);
        if (establishment.isEmpty()) {
            return null;
        }

        Establishment establishmentEntity = establishment.get();

        // Update only non-null fields
        if (establishmentDto.name() != null) {
            establishmentEntity.setName(establishmentDto.name());
        }
        if (establishmentDto.email() != null) {
            establishmentEntity.setEmail(establishmentDto.email());
        }
        if (establishmentDto.phone() != null) {
            establishmentEntity.setPhone(establishmentDto.phone());
        }
        if (establishmentDto.cnpj() != null) {
            establishmentEntity.setCnpj(establishmentDto.cnpj());
        }
        if (establishmentDto.valuePerPoint() != null) {
            establishmentEntity.setValuePerPoint(establishmentDto.valuePerPoint());
        }

        return EstablishmentDto.toDto(establishmentRepository.save(establishmentEntity));
    }
}

