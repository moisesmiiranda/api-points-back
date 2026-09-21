package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.AdjustPointsRequestDto;
import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.dto.ImportClientRequestDto;
import com.mmiranda.pointsbackapi.dto.LedgerEntryDto;
import com.mmiranda.pointsbackapi.dto.PageDto;
import com.mmiranda.pointsbackapi.dto.RedeemPreviewDto;
import com.mmiranda.pointsbackapi.service.ClientService;
import com.mmiranda.pointsbackapi.validation.OnCreate;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @PostMapping
    public ClientDto createClient(@Validated(OnCreate.class) @RequestBody ClientDto clientDto) {
        return clientService.createClient(clientDto);
    }

    @GetMapping("/search")
    public List<ClientDto> searchClients(@RequestParam(required = false) String cpf,
                                         @RequestParam(required = false) String phone,
                                         @RequestParam(required = false) Long establishmentId) {
        return clientService.searchClients(cpf, phone, establishmentId);
    }

    @PostMapping("/import")
    public ClientDto importFromGroup(@Valid @RequestBody ImportClientRequestDto request) {
        return clientService.importFromGroup(request);
    }

    @GetMapping("/all")
    public List<ClientDto> listAllClients() {
        return clientService.listAllClients();
    }

    @GetMapping("/{id}")
    public ClientDto getClient(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

    @PostMapping("/{id}/points/adjust")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ESTABLISHMENT_OWNER')")
    public ClientDto adjustPoints(@PathVariable Long id, @Valid @RequestBody AdjustPointsRequestDto request) {
        return clientService.adjustPoints(id, request);
    }

    @GetMapping("/{id}/statement")
    public PageDto<LedgerEntryDto> statement(@PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return clientService.statement(id, page, size);
    }

    @GetMapping("/{id}/redeemable")
    public RedeemPreviewDto redeemable(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return clientService.redeemPreview(id, amount);
    }

    @PutMapping("/{id}")
    public ClientDto updateClient(@PathVariable Long id, @Valid @RequestBody ClientDto clientDto) {
        return clientService.updateClient(id, clientDto);
    }

}
