package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @SuppressWarnings("null")
    public ClientDto getClientById(Long id) {
        return clientRepository.findById(id)
                .map(ClientDto::toDto)
                .orElse(null);
    }

    public ClientDto createClient(ClientDto clientDto) {
        Client entity = ClientDto.toEntity(clientDto);
        if (entity == null) {
            return null;
        }
        return ClientDto.toDto(clientRepository.save(entity));
    }

    public List<ClientDto> listAllClients() {
        return clientRepository.findAll().stream()
                .map(ClientDto::toDto)
                .toList();
    }

    @SuppressWarnings("null")
    public boolean addPoints(Long clientId, int points) {
        var client = clientRepository.findById(clientId);
        if (client.isEmpty()) {
            return false;
        }
        Client clientEntity = client.get();
        int currentPoints = clientEntity.getPoints() != null ? clientEntity.getPoints() : 0;
        clientEntity.setPoints(currentPoints + points);
        clientRepository.save(clientEntity);
        return true;
    }
}
