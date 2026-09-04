package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.EstablishmentDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstablishmentService {

    @Autowired
    private EstablishmentRepository establishmentRepository;

    public EstablishmentDto getEstablishmentById(Long id) {
        SecurityUtils.requireEstablishmentAccess(id);
        return establishmentRepository.findById(id)
                .map(EstablishmentDto::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find establishment with id: " + id));
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
        if (SecurityUtils.getCurrentUser().role() == Role.ESTABLISHMENT_STAFF) {
            throw new ForbiddenException("Staff accounts cannot update establishment details");
        }
        SecurityUtils.requireEstablishmentAccess(establishmentId);

        var establishment = establishmentRepository.findById(establishmentId);
        if (establishment.isEmpty()) {
            throw new ResourceNotFoundException("Cannot find establishment with id: " + establishmentId);
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

