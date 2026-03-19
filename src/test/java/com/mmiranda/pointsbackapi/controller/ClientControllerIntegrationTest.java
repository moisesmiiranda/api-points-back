package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
}

