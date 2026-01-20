package com.mmiranda.pointsbackapi.service;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.repository.ClientRepository;

public class ClientServiceTest {

    @Mock
    private ClientRepository repository;

    @InjectMocks
    private ClientService service;

    @BeforeEach
    void setUp() {
        org.mockito.MockitoAnnotations.openMocks(this);        
    }

    Client clientTest = buildClient();

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
        assert result != null;
        assert result.name().equals("Test Client");
        assert result.email().equals("test@example.com");

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
        assert result == null;

    }

    @Test
    void testCreateClient() {
        // Arrange       
        ClientDto clientDto = ClientDto.toDto(clientTest);
        when(repository.save(clientTest)).thenReturn(clientTest);

        // Act
        ClientDto result = service.createClient(clientDto);

        // Assert
        assert result != null;
        assert result.name().equals("Test Client");
        assert result.email().equals("test@example.com");
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