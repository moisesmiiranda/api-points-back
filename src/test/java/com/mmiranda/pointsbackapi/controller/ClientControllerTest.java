package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.AdjustPointsRequestDto;
import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.dto.LedgerEntryDto;
import com.mmiranda.pointsbackapi.dto.PageDto;
import com.mmiranda.pointsbackapi.dto.RedeemPreviewDto;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private ClientService clientService;

    @InjectMocks
    private ClientController clientController;

    private ClientDto clientDto;
    private Client client;

    @BeforeEach
    void setUp() {
        client = buildClient();
        clientDto = buildClientDto();
    }

    @Test
    void testCreateClient() {
        // Arrange
        when(clientService.createClient(any(ClientDto.class)))
                .thenReturn(clientDto);

        // Act
        ClientDto result = clientController.createClient(clientDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
        assertEquals("test@example.com", result.email());
        verify(clientService, times(1)).createClient(any(ClientDto.class));
    }

    @Test
    void testListAllClients() {
        // Arrange
        Long clientId = 1L;
        ClientDto clientDto2 = new ClientDto(clientId,"Test Client 2", "test2@example.com",
                "0987654321", "111.444.777-35", 200, 1L);

        when(clientService.listAllClients())
                .thenReturn(Arrays.asList(clientDto, clientDto2));

        // Act
        List<ClientDto> result = clientController.listAllClients();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Client", result.get(0).name());
        assertEquals("Test Client 2", result.get(1).name());
        verify(clientService, times(1)).listAllClients();
    }

    @Test
    void testGetClient() {
        // Arrange
        Long clientId = 1L;

        when(clientService.getClientById(clientId))
                .thenReturn(clientDto);

        // Act
        ClientDto result = clientController.getClient(clientId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
        assertEquals("test@example.com", result.email());
        verify(clientService, times(1)).getClientById(clientId);
    }

    @Test
    void testAdjustPoints() {
        Long clientId = 1L;
        AdjustPointsRequestDto request = new AdjustPointsRequestDto(10, "Welcome bonus");
        ClientDto adjusted = new ClientDto(clientId, "Test Client", "test@example.com", "1234567890",
                "529.982.247-25", 10, 1L);
        when(clientService.adjustPoints(clientId, request)).thenReturn(adjusted);

        ClientDto result = clientController.adjustPoints(clientId, request);

        assertEquals(10, result.points());
        verify(clientService, times(1)).adjustPoints(clientId, request);
    }

    @Test
    void testStatement() {
        PageDto<LedgerEntryDto> page = new PageDto<>(java.util.List.of(), 0, 20, 0, 0);
        when(clientService.statement(1L, 0, 20)).thenReturn(page);

        assertEquals(page, clientController.statement(1L, 0, 20));
    }

    @Test
    void testRedeemable() {
        RedeemPreviewDto preview = new RedeemPreviewDto(100, new java.math.BigDecimal("10.00"), RewardMode.DISCOUNT,
                new java.math.BigDecimal("0.1000"), 50, new java.math.BigDecimal("5.00"));
        when(clientService.redeemPreview(1L, new java.math.BigDecimal("20"))).thenReturn(preview);

        assertEquals(preview, clientController.redeemable(1L, new java.math.BigDecimal("20")));
    }

    @Test
    void testUpdateClient() {
        // Arrange
        Long clientId = 1L;
        ClientDto updateDto = new ClientDto(
                clientId,
                "Updated Client",
                "updated@example.com",
                "9999999999",
                "529.982.247-25",
                500,
                1L
        );

        when(clientService.updateClient(clientId, updateDto))
                .thenReturn(updateDto);

        // Act
        ClientDto result = clientController.updateClient(clientId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Client", result.name());
        assertEquals("updated@example.com", result.email());
        verify(clientService, times(1)).updateClient(clientId, updateDto);
    }

    @Test
    void testUpdateClientPartial() {
        // Arrange
        Long clientId = 1L;
        ClientDto updateDto = new ClientDto(
                clientId,
                "Updated Name",
                null,
                null,
                null,
                null,
                null
        );

        ClientDto responseDto = new ClientDto(
                clientId,
                "Updated Name",
                "test@example.com",
                "1234567890",
                "529.982.247-25",
                100,
                1L
        );

        when(clientService.updateClient(clientId, updateDto))
                .thenReturn(responseDto);

        // Act
        ClientDto result = clientController.updateClient(clientId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.name());
        verify(clientService, times(1)).updateClient(clientId, updateDto);
    }

    @Test
    void testUpdateClientNotFound() {
        // Arrange
        Long clientId = 999L;
        ClientDto updateDto = new ClientDto(
                clientId,
                "Updated Client",
                "updated@example.com",
                "9999999999",
                "529.982.247-25",
                500,
                1L
        );

        when(clientService.updateClient(clientId, updateDto))
                .thenReturn(null);

        // Act
        ClientDto result = clientController.updateClient(clientId, updateDto);

        // Assert
        assertNull(result);
        verify(clientService, times(1)).updateClient(clientId, updateDto);
    }

    private ClientDto buildClientDto() {
        Long clientId = 1L;
        return new ClientDto(
                clientId,
                "Test Client",
                "test@example.com",
                "1234567890",
                "529.982.247-25",
                100,
                1L
        );
    }

    private Client buildClient() {
        return Client.builder()
                .id(1L)
                .name("Test Client")
                .email("test@example.com")
                .phone("1234567890")
                .person(com.mmiranda.pointsbackapi.model.Person.builder().cpf("52998224725").build())
                .points(100)
                .build();
    }
}
