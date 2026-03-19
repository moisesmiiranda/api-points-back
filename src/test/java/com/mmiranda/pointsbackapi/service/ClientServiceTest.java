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