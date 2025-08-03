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

    public ClientDto getClientById(Long id) {
        return clientRepository.findById(id)
                .map(ClientDto::toDto)
                .orElse(null);
    }

    public Client createClient(ClientDto clientDto) {
        Client client = ClientDto.toEntity(clientDto);
        return clientRepository.save(client);
    }

    public List<Client> listAllClients() {
        return clientRepository.findAll();
    }

    public void addPoints(Long clientId, int points) {
        clientRepository.findById(clientId).ifPresent(client -> {
            int currentPoints = client.getPoints() != null ? client.getPoints() : 0;
            client.setPoints(currentPoints + points);
            clientRepository.save(client);
        });
    }
}
