package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.AssignGroupRequestDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentGroupDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentSharingDto;
import com.mmiranda.pointsbackapi.dto.UpdateSharingRequestDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentGroup;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.repository.EstablishmentGroupRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Establishment groups (for example the branches of one chain) and each establishment's choice
 * to share its clients with the rest of its group. Only PLATFORM_ADMIN decides group membership
 * (the controller enforces it); the owner of an establishment decides whether it shares.
 */
@Service
public class EstablishmentGroupService {

    private final EstablishmentGroupRepository groupRepository;
    private final EstablishmentRepository establishmentRepository;

    public EstablishmentGroupService(EstablishmentGroupRepository groupRepository,
                                      EstablishmentRepository establishmentRepository) {
        this.groupRepository = groupRepository;
        this.establishmentRepository = establishmentRepository;
    }

    @Transactional
    public EstablishmentGroupDto createGroup(EstablishmentGroupDto request) {
        EstablishmentGroup group = EstablishmentGroup.builder().name(request.name()).build();
        return EstablishmentGroupDto.toDto(groupRepository.save(group));
    }

    public List<EstablishmentGroupDto> listGroups() {
        return groupRepository.findAll().stream().map(EstablishmentGroupDto::toDto).toList();
    }

    /** Puts an establishment in a group, or takes it out (which also stops it sharing). */
    @Transactional
    public EstablishmentSharingDto assignGroup(Long establishmentId, AssignGroupRequestDto request) {
        Establishment establishment = requireEstablishment(establishmentId);
        if (request.groupId() == null) {
            establishment.setGroup(null);
            establishment.setShareClients(false);
        } else {
            establishment.setGroup(groupRepository.findById(request.groupId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cannot find group with id: " + request.groupId())));
        }
        return EstablishmentSharingDto.toDto(establishmentRepository.save(establishment));
    }

    public EstablishmentSharingDto getSharing(Long establishmentId) {
        SecurityUtils.requireEstablishmentAccess(establishmentId);
        return EstablishmentSharingDto.toDto(requireEstablishment(establishmentId));
    }

    @Transactional
    public EstablishmentSharingDto updateSharing(Long establishmentId, UpdateSharingRequestDto request) {
        if (SecurityUtils.getCurrentUser().role() == Role.ESTABLISHMENT_STAFF) {
            throw new ForbiddenException("Staff accounts cannot change client sharing");
        }
        SecurityUtils.requireEstablishmentAccess(establishmentId);
        Establishment establishment = requireEstablishment(establishmentId);
        if (request.shareClients() && establishment.getGroup() == null) {
            throw new IllegalArgumentException("The establishment must belong to a group before it can share clients");
        }
        establishment.setShareClients(request.shareClients());
        return EstablishmentSharingDto.toDto(establishmentRepository.save(establishment));
    }

    private Establishment requireEstablishment(Long id) {
        return establishmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cannot find establishment with id: " + id));
    }
}
