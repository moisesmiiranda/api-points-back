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

    @SuppressWarnings("null")
    public ClientDto updateClient(Long clientId, ClientDto clientDto) {
        var client = clientRepository.findById(clientId);
        if (client.isEmpty()) {
            return null;
        }

        Client clientEntity = client.get();

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
}
