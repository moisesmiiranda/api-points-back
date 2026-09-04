package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.AuthenticatedUser;
import com.mmiranda.pointsbackapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EstablishmentRepository establishmentRepository;

    public ClientDto getClientById(Long id) {
        Client client = requireScopedClient(id);
        return client != null ? ClientDto.toDto(client) : null;
    }

    public ClientDto createClient(ClientDto clientDto) {
        Establishment establishment = resolveEstablishmentForWrite(clientDto.establishmentId());
        Client entity = ClientDto.toEntity(clientDto);
        entity.setEstablishment(establishment);
        return ClientDto.toDto(clientRepository.save(entity));
    }

    public List<ClientDto> listAllClients() {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        List<Client> clients = caller.isPlatformAdmin()
                ? clientRepository.findAll()
                : clientRepository.findAllByEstablishmentId(caller.establishmentId());
        return clients.stream()
                .map(ClientDto::toDto)
                .toList();
    }

    public boolean addPoints(Long clientId, int points) {
        Client clientEntity = requireScopedClient(clientId);
        if (clientEntity == null) {
            return false;
        }
        int currentPoints = clientEntity.getPoints() != null ? clientEntity.getPoints() : 0;
        clientEntity.setPoints(currentPoints + points);
        clientRepository.save(clientEntity);
        return true;
    }

    @SuppressWarnings("null")
    public ClientDto updateClient(Long clientId, ClientDto clientDto) {
        Client clientEntity = requireScopedClient(clientId);
        if (clientEntity == null) {
            return null;
        }

        // Update only non-null fields
        if (clientDto.name() != null) {
            clientEntity.setName(clientDto.name());
        }
        if (clientDto.email() != null) {
            clientEntity.setEmail(clientDto.email());
        }
        if (clientDto.phone() != null) {
            clientEntity.setPhone(clientDto.phone());
        }
        if (clientDto.cpf() != null) {
            clientEntity.setCpf(clientDto.cpf());
        }
        if (clientDto.points() != null) {
            clientEntity.setPoints(clientDto.points());
        }

        return ClientDto.toDto(clientRepository.save(clientEntity));
    }

    /**
     * PLATFORM_ADMIN sees a plain not-found (null) for a missing id. Every other role gets
     * an identical ForbiddenException whether the id belongs to another establishment or
     * doesn't exist at all, so a 403 never leaks which case it was.
     */
    private Client requireScopedClient(Long id) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        if (caller.isPlatformAdmin()) {
            return clientRepository.findById(id).orElse(null);
        }
        return clientRepository.findByIdAndEstablishmentId(id, caller.establishmentId())
                .orElseThrow(() -> new ForbiddenException("You do not have access to this client"));
    }

    private Establishment resolveEstablishmentForWrite(Long requestedEstablishmentId) {
        AuthenticatedUser caller = SecurityUtils.getCurrentUser();
        Long establishmentId = requestedEstablishmentId;

        if (caller.isPlatformAdmin()) {
            if (establishmentId == null) {
                throw new IllegalArgumentException("establishmentId is required");
            }
        } else if (establishmentId == null) {
            establishmentId = caller.establishmentId();
        } else if (!caller.belongsToEstablishment(establishmentId)) {
            throw new ForbiddenException("You do not have access to this establishment");
        }

        Long resolvedEstablishmentId = establishmentId;
        return establishmentRepository.findById(resolvedEstablishmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cannot find establishment with id: " + resolvedEstablishmentId));
    }
}
