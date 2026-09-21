package com.mmiranda.pointsbackapi.dto;

import com.mmiranda.pointsbackapi.model.Establishment;

/** The group an establishment belongs to (if any) and whether it shares its clients with that group. */
public record EstablishmentSharingDto(
        Long establishmentId,
        Long groupId,
        String groupName,
        boolean shareClients
) {
    public static EstablishmentSharingDto toDto(Establishment establishment) {
        return new EstablishmentSharingDto(
                establishment.getId(),
                establishment.getGroup() != null ? establishment.getGroup().getId() : null,
                establishment.getGroup() != null ? establishment.getGroup().getName() : null,
                establishment.isShareClients());
    }
}
