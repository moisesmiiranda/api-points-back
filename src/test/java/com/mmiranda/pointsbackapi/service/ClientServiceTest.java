package com.mmiranda.pointsbackapi.service;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ClientService service;

    private Client clientTest;

    @BeforeEach
    void setUp() {
        clientTest = buildClient();
        assertNotNull(clientTest);
    }


    @Test
    void testGetClientById() {
        // Arrange
        Long clientId = 1L;
        
        // Act
        when(repository.findById(clientId))
        .thenReturn(java.util.Optional.
            of(clientTest));
        
        ClientDto result = service.getClientById(clientId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
        assertEquals("test@example.com", result.email());
    }

    @Test
    void testFailGetClientById() {
        // Arrange
        Long clientId = 1L;
        // Act
        when(repository.findById(clientId))
        .thenReturn(java.util.Optional.empty());
        
        ClientDto result = service.getClientById(clientId);

        // Assert
        assertNull(result);
    }

    @Test
    void testCreateClient() {
        // Arrange       
        ClientDto clientDto = ClientDto.toDto(clientTest);
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        ClientDto result = service.createClient(clientDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
        assertEquals("test@example.com", result.email());
    }

    @Test
    void testCreateClientWithNullEntity() {
        // Arrange
        ClientDto clientDto = new ClientDto(null, null, null, null, null);

        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        ClientDto result = service.createClient(clientDto);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).save(any(Client.class));
    }

    @Test
    void testListAllClients() {
        // Arrange
        Client client2 = Client.builder()
                .id(2L)
                .name("Test Client 2")
                .email("test2@example.com")
                .phone("0987654321")
                .cpf("987.654.321-00")
                .points(200)
                .build();

        when(repository.findAll()).thenReturn(Arrays.asList(clientTest, client2));

        // Act
        List<ClientDto> result = service.listAllClients();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Client", result.get(0).name());
        assertEquals("Test Client 2", result.get(1).name());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testAddPoints() {
        // Arrange
        Long clientId = 1L;
        int pointsToAdd = 50;
        clientTest.setPoints(100);

        when(repository.findById(clientId)).thenReturn(java.util.Optional.of(clientTest));
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        boolean result = service.addPoints(clientId, pointsToAdd);

        // Assert
        assertEquals(true, result);
        assertEquals(150, clientTest.getPoints());
        verify(repository, times(1)).findById(clientId);
        verify(repository, times(1)).save(clientTest);
    }

    @Test
    void testAddPointsClientNotFound() {
        // Arrange
        Long clientId = 999L;
        int pointsToAdd = 50;

        when(repository.findById(clientId)).thenReturn(java.util.Optional.empty());

        // Act
        boolean result = service.addPoints(clientId, pointsToAdd);

        // Assert
        assertEquals(false, result);
        verify(repository, times(1)).findById(clientId);
        verify(repository, times(0)).save(any());
    }

    @Test
    void testAddPointsWithNullCurrentPoints() {
        // Arrange
        Long clientId = 1L;
        int pointsToAdd = 50;
        clientTest.setPoints(null);

        when(repository.findById(clientId)).thenReturn(java.util.Optional.of(clientTest));
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        boolean result = service.addPoints(clientId, pointsToAdd);

        // Assert
        assertEquals(true, result);
        assertEquals(50, clientTest.getPoints());
        verify(repository, times(1)).findById(clientId);
        verify(repository, times(1)).save(clientTest);
    }

    public Client buildClient() {
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