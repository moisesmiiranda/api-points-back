package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void testUpdateClientPointsSuccess() throws Exception {
        // Arrange
        Long clientId = 1L;
        int points = 10;

        when(clientService.addPoints(clientId, points))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(put("/clients/{id}/points", clientId)
                .param("points", String.valueOf(points)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testUpdateClientPointsNotFound() throws Exception {
        // Arrange
        Long clientId = 999L;
        int points = 10;

        when(clientService.addPoints(clientId, points))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(put("/clients/{id}/points", clientId)
                .param("points", String.valueOf(points)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testUpdateClientSuccess() throws Exception {
        // Arrange
        Long clientId = 1L;
        String updatePayload = """
                {
                    "name": "Updated Client",
                    "email": "updated@example.com",
                    "phone": "9999999999",
                    "cpf": "999.999.999-99",
                    "points": 500
                }
                """;

        when(clientService.updateClient(eq(clientId), any()))
                .thenReturn(new com.mmiranda.pointsbackapi.dto.ClientDto(
                        "Updated Client",
                        "updated@example.com",
                        "9999999999",
                        "999.999.999-99",
                        500
                ));

        // Act & Assert
        mockMvc.perform(put("/clients/{id}", clientId)
                .contentType("application/json")
                .content(updatePayload))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateClientNotFound() throws Exception {
        // Arrange
        Long clientId = 999L;
        String updatePayload = """
                {
                    "name": "Updated Client",
                    "email": "updated@example.com",
                    "phone": "9999999999",
                    "cpf": "999.999.999-99",
                    "points": 500
                }
                """;

        when(clientService.updateClient(eq(clientId), any()))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(put("/clients/{id}", clientId)
                .contentType("application/json")
                .content(updatePayload))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateClientPartialFields() throws Exception {
        // Arrange
        Long clientId = 1L;
        String updatePayload = """
                {
                    "name": "Updated Name",
                    "email": null,
                    "phone": null,
                    "cpf": null,
                    "points": null
                }
                """;

        when(clientService.updateClient(eq(clientId), any()))
                .thenReturn(new com.mmiranda.pointsbackapi.dto.ClientDto(
                        "Updated Name",
                        "test@example.com",
                        "1234567890",
                        "123.456.789-00",
                        100
                ));

        // Act & Assert
        mockMvc.perform(put("/clients/{id}", clientId)
                .contentType("application/json")
                .content(updatePayload))
                .andExpect(status().isOk());
    }
}
