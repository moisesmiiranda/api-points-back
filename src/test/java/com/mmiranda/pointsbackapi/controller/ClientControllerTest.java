package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.ClientDto;
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
        ClientDto clientDto2 = new ClientDto("Test Client 2", "test2@example.com", 
                "0987654321", "987.654.321-00", 200);

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
    void testUpdateClientPoints() {
        // Arrange
        Long clientId = 1L;
        int pointsToAdd = 10;

        when(clientService.addPoints(clientId, pointsToAdd))
                .thenReturn(true);

        // Act
        boolean result = clientController.updateClientPoints(clientId, pointsToAdd);

        // Assert
        assertEquals(true, result);
        verify(clientService, times(1)).addPoints(clientId, pointsToAdd);
    }

    @Test
    void testUpdateClientPointsClientNotFound() {
        // Arrange
        Long clientId = 999L;
        int pointsToAdd = 10;

        when(clientService.addPoints(clientId, pointsToAdd))
                .thenReturn(false);

        // Act
        boolean result = clientController.updateClientPoints(clientId, pointsToAdd);

        // Assert
        assertEquals(false, result);
        verify(clientService, times(1)).addPoints(clientId, pointsToAdd);
    }

    private ClientDto buildClientDto() {
        return new ClientDto(
                "Test Client",
                "test@example.com",
                "1234567890",
                "123.456.789-00",
                100
        );
    }

    private Client buildClient() {
        return Client.builder()
                .id(1L)
                .name("Test Client")
                .email("test@example.com")
                .phone("1234567890")
                .cpf("123.456.789-00")
                .points(100)
                .build();
    }
}

