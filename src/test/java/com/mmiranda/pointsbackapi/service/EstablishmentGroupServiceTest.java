package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.AssignGroupRequestDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentGroupDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentSharingDto;
import com.mmiranda.pointsbackapi.dto.UpdateSharingRequestDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentGroup;
import com.mmiranda.pointsbackapi.repository.EstablishmentGroupRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablishmentGroupServiceTest {

    @Mock
    private EstablishmentGroupRepository groupRepository;

    @Mock
    private EstablishmentRepository establishmentRepository;

    @InjectMocks
    private EstablishmentGroupService service;

    private Establishment establishment;
    private EstablishmentGroup group;

    @BeforeEach
    void setUp() {
        establishment = new Establishment();
        establishment.setId(1L);
        group = EstablishmentGroup.builder().id(9L).name("Chain").build();
        TestAuth.asPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    @Test
    void createsAndListsGroups() {
        when(groupRepository.save(any(EstablishmentGroup.class))).thenReturn(group);
        when(groupRepository.findAll()).thenReturn(List.of(group));

        EstablishmentGroupDto created = service.createGroup(new EstablishmentGroupDto(null, "Chain"));

        assertEquals(9L, created.id());
        assertEquals("Chain", created.name());
        assertEquals(1, service.listGroups().size());
    }

    @Test
    void assignsAnEstablishmentToAGroup() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(groupRepository.findById(9L)).thenReturn(Optional.of(group));
        when(establishmentRepository.save(establishment)).thenReturn(establishment);

        EstablishmentSharingDto result = service.assignGroup(1L, new AssignGroupRequestDto(9L));

        assertEquals(9L, result.groupId());
        assertEquals("Chain", result.groupName());
        assertFalse(result.shareClients());
    }

    @Test
    void removingAnEstablishmentFromItsGroupAlsoStopsSharing() {
        establishment.setGroup(group);
        establishment.setShareClients(true);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(establishmentRepository.save(establishment)).thenReturn(establishment);

        EstablishmentSharingDto result = service.assignGroup(1L, new AssignGroupRequestDto(null));

        assertNull(result.groupId());
        assertNull(result.groupName());
        assertFalse(result.shareClients());
    }

    @Test
    void assigningToAnUnknownGroupOrEstablishmentIsNotFound() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(groupRepository.findById(404L)).thenReturn(Optional.empty());
        when(establishmentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.assignGroup(1L, new AssignGroupRequestDto(404L)));
        assertThrows(ResourceNotFoundException.class, () -> service.assignGroup(2L, new AssignGroupRequestDto(9L)));
    }

    @Test
    void ownerCanReadAndChangeSharingOfTheirOwnEstablishment() {
        TestAuth.clear();
        TestAuth.asEstablishmentOwner(1L);
        establishment.setGroup(group);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(establishmentRepository.save(establishment)).thenReturn(establishment);

        assertFalse(service.getSharing(1L).shareClients());
        assertTrue(service.updateSharing(1L, new UpdateSharingRequestDto(true)).shareClients());
        assertFalse(service.updateSharing(1L, new UpdateSharingRequestDto(false)).shareClients());
    }

    @Test
    void sharingRequiresAGroup() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));

        assertThrows(IllegalArgumentException.class,
                () -> service.updateSharing(1L, new UpdateSharingRequestDto(true)));
    }

    @Test
    void staffCannotChangeSharingButOtherEstablishmentsAreOffLimits() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        assertThrows(ForbiddenException.class, () -> service.updateSharing(1L, new UpdateSharingRequestDto(true)));

        TestAuth.clear();
        TestAuth.asEstablishmentOwner(2L);
        assertThrows(ForbiddenException.class, () -> service.getSharing(1L));
        assertThrows(ForbiddenException.class, () -> service.updateSharing(1L, new UpdateSharingRequestDto(true)));
    }

    @Test
    void unknownEstablishmentIsNotFound() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getSharing(1L));
    }
}
