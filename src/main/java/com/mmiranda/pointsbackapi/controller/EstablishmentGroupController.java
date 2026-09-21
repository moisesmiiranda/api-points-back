package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.AssignGroupRequestDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentGroupDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentSharingDto;
import com.mmiranda.pointsbackapi.dto.UpdateSharingRequestDto;
import com.mmiranda.pointsbackapi.service.EstablishmentGroupService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EstablishmentGroupController {

    private final EstablishmentGroupService groupService;

    public EstablishmentGroupController(EstablishmentGroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/establishment-groups")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public EstablishmentGroupDto createGroup(@Valid @RequestBody EstablishmentGroupDto request) {
        return groupService.createGroup(request);
    }

    @GetMapping("/establishment-groups")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<EstablishmentGroupDto> listGroups() {
        return groupService.listGroups();
    }

    @PutMapping("/establishments/{id}/group")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public EstablishmentSharingDto assignGroup(@PathVariable Long id, @RequestBody AssignGroupRequestDto request) {
        return groupService.assignGroup(id, request);
    }

    @GetMapping("/establishments/{id}/sharing")
    public EstablishmentSharingDto getSharing(@PathVariable Long id) {
        return groupService.getSharing(id);
    }

    @PutMapping("/establishments/{id}/sharing")
    public EstablishmentSharingDto updateSharing(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateSharingRequestDto request) {
        return groupService.updateSharing(id, request);
    }
}
