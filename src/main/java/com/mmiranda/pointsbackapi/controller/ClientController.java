package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping
    public ClientDto createClient(@RequestBody ClientDto clientDto) {
        return clientService.createClient(clientDto);
    }

    @GetMapping("/all")
    public List<ClientDto> listAllClients() {
        return clientService.listAllClients();
    }

    @GetMapping("/{id}")
    public ClientDto getClient(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

}
