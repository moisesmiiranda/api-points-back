package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Security filters are disabled here: this slice only verifies controller <-> service wiring.
// Authentication/authorization behavior is covered by SecurityConfig/JWT-focused tests.
@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void testAdjustPointsSuccess() throws Exception {
        Long clientId = 1L;
        when(clientService.adjustPoints(eq(clientId), any()))
                .thenReturn(new com.mmiranda.pointsbackapi.dto.ClientDto(
                        clientId, "Client", "c@example.com", "11999990000", "529.982.247-25", 10, 1L));

        mockMvc.perform(post("/clients/{id}/points/adjust", clientId)
                .contentType("application/json")
                .content("{\"points\":10,\"reason\":\"Welcome bonus\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(10));
    }

    @Test
    void testAdjustPointsRequiresAReason() throws Exception {
        mockMvc.perform(post("/clients/{id}/points/adjust", 1L)
                .contentType("application/json")
                .content("{\"points\":10}"))
                .andExpect(status().isBadRequest());
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
                    "cpf": "529.982.247-25",
                    "points": 500
                }
                """;

        when(clientService.updateClient(eq(clientId), any()))
                .thenReturn(new com.mmiranda.pointsbackapi.dto.ClientDto(
                        clientId,
                        "Updated Client",
                        "updated@example.com",
                        "9999999999",
                        "529.982.247-25",
                        500,
                        1L
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
                    "cpf": "529.982.247-25",
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
                        clientId,
                        "Updated Name",
                        "test@example.com",
                        "1234567890",
                        "529.982.247-25",
                        100,
                        1L
                ));

        // Act & Assert
        mockMvc.perform(put("/clients/{id}", clientId)
                .contentType("application/json")
                .content(updatePayload))
                .andExpect(status().isOk());
    }
}
