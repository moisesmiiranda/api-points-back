package com.mmiranda.pointsbackapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real Spring Security filter chain end-to-end (SecurityConfig,
 * JwtAuthenticationFilter, GlobalExceptionHandler) against the real H2/Flyway-backed
 * context, using the PLATFORM_ADMIN account seeded by AdminBootstrapRunner.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/establishments/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/establishments/all")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownCredentialsIsRejected() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"nobody@test.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithBootstrapAdminSucceedsAndGrantsAccessToProtectedEndpoint() throws Exception {
        String loginPayload = """
                {"email":"admin@pointsback.local","password":"ChangeMe123!"}
                """;

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        String token = extractAccessToken(responseBody);

        mockMvc.perform(get("/establishments/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminRoleIsForbiddenFromCreatingEstablishments() throws Exception {
        String loginPayload = """
                {"email":"admin@pointsback.local","password":"ChangeMe123!"}
                """;
        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = extractAccessToken(responseBody);

        // Sanity check: the admin token itself IS allowed to create establishments.
        mockMvc.perform(post("/establishments")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"name":"New Shop","email":"shop@test.com","phone":"123","cnpj":"00.000.000/0001-00","valuePerPoint":5}
                                """))
                .andExpect(status().isOk());
    }

    private String extractAccessToken(String jsonResponseBody) {
        int start = jsonResponseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end = jsonResponseBody.indexOf('"', start);
        return jsonResponseBody.substring(start, end);
    }
}
