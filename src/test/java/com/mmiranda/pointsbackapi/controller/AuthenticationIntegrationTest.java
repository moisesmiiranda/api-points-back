package com.mmiranda.pointsbackapi.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void establishmentOwnerManagesOwnStaffButNotOtherAccounts() throws Exception {
        String adminToken = extractAccessToken(login("admin@pointsback.local", "ChangeMe123!"));

        // Admin creates an ESTABLISHMENT_OWNER bound to the seeded establishment 1.
        mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"name":"Owner U","email":"owner.u@test.com","password":"Passw0rd!","role":"ESTABLISHMENT_OWNER","establishmentId":1}
                                """))
                .andExpect(status().isOk());

        // Admin can list every account.
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists());

        String ownerToken = extractAccessToken(login("owner.u@test.com", "Passw0rd!"));

        // Owner creates a staff member for their own establishment.
        String staffJson = mockMvc.perform(post("/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("""
                                {"name":"Staff U","email":"staff.u@test.com","password":"Passw0rd!","role":"ESTABLISHMENT_STAFF","establishmentId":1}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long staffId = Long.parseLong(staffJson.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // Owner lists users: only their own establishment's STAFF, never the admin/owner rows.
        mockMvc.perform(get("/users").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("staff.u@test.com")))
                .andExpect(jsonPath("$[*].role", everyItem(is("ESTABLISHMENT_STAFF"))))
                .andExpect(jsonPath("$[*].establishmentId", everyItem(is(1))));

        // Owner edits and deactivates that staff member.
        mockMvc.perform(put("/users/" + staffId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"name\":\"Staff U Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Staff U Renamed"));
        mockMvc.perform(delete("/users/" + staffId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Owner cannot touch the platform admin account (id 1).
        mockMvc.perform(put("/users/1")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType("application/json")
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/users/1").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
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

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String extractAccessToken(String jsonResponseBody) {
        int start = jsonResponseBody.indexOf("\"accessToken\":\"") + "\"accessToken\":\"".length();
        int end = jsonResponseBody.indexOf('"', start);
        return jsonResponseBody.substring(start, end);
    }
}
