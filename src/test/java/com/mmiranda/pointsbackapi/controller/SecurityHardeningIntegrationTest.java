package com.mmiranda.pointsbackapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level checks (real filter chain, method security, validation, exception handler, H2 + Flyway)
 * for the hardening done in the mvp-01-secure-foundation change. Every test creates its own
 * establishments/clients/users so it does not depend on, or disturb, the seeded demo data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityHardeningIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";
    private static final AtomicLong SEQUENCE = new AtomicLong(700_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    // ---------------------------------------------------------------- authentication / authorization

    @Test
    void everyBusinessEndpointRejectsAnonymousCallers() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/purchases")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/clients/all")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/establishments/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/purchases").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffIsForbiddenNotServerErrorOnAdminAndOwnerOperations() throws Exception {
        long establishmentId = createEstablishment(adminToken(), 10);
        String staffToken = login(createUser(adminToken(), "ESTABLISHMENT_STAFF", establishmentId));

        // @PreAuthorize denials must surface as 403 (not swallowed into a 500 by the catch-all handler)
        mockMvc.perform(post("/establishments").header("Authorization", bearer(staffToken))
                        .contentType("application/json").content(establishmentJson(10)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/users").header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/users").header("Authorization", bearer(staffToken))
                        .contentType("application/json").content(userJson("x" + SEQUENCE.incrementAndGet() + "@test.com",
                                "ESTABLISHMENT_STAFF", establishmentId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/establishments/all").header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotUpdateEstablishmentButOwnerCan() throws Exception {
        long establishmentId = createEstablishment(adminToken(), 10);
        String staffToken = login(createUser(adminToken(), "ESTABLISHMENT_STAFF", establishmentId));
        String ownerToken = login(createUser(adminToken(), "ESTABLISHMENT_OWNER", establishmentId));

        mockMvc.perform(put("/establishments/" + establishmentId).header("Authorization", bearer(staffToken))
                        .contentType("application/json").content("{\"name\":\"Hacked\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/establishments/" + establishmentId).header("Authorization", bearer(ownerToken))
                        .contentType("application/json").content("{\"name\":\"Renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void deactivatedUserLosesAccessImmediatelyEvenWithAValidToken() throws Exception {
        long establishmentId = createEstablishment(adminToken(), 10);
        String email = createUser(adminToken(), "ESTABLISHMENT_STAFF", establishmentId);
        long userId = userIdOf(email);
        String staffToken = login(email);

        mockMvc.perform(get("/users/me").header("Authorization", bearer(staffToken))).andExpect(status().isOk());

        mockMvc.perform(delete("/users/" + userId).header("Authorization", bearer(adminToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me").header("Authorization", bearer(staffToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeTakesEffectWithoutWaitingForTokenExpiry() throws Exception {
        long establishmentId = createEstablishment(adminToken(), 10);
        String email = createUser(adminToken(), "ESTABLISHMENT_OWNER", establishmentId);
        long userId = userIdOf(email);
        String ownerToken = login(email);

        mockMvc.perform(get("/users").header("Authorization", bearer(ownerToken))).andExpect(status().isOk());

        mockMvc.perform(put("/users/" + userId).header("Authorization", bearer(adminToken()))
                        .contentType("application/json")
                        .content("{\"role\":\"ESTABLISHMENT_STAFF\",\"establishmentId\":" + establishmentId + "}"))
                .andExpect(status().isOk());

        // Same token, but the account is STAFF now, so the owner-only endpoint is forbidden
        mockMvc.perform(get("/users").header("Authorization", bearer(ownerToken))).andExpect(status().isForbidden());
    }

    @Test
    void loginIsThrottledAfterRepeatedFailures() throws Exception {
        String email = "brute" + SEQUENCE.incrementAndGet() + "@test.com";
        String body = "{\"email\":\"" + email + "\",\"password\":\"wrong\"}";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login").contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void loginWithMissingFieldsIsABadRequest() throws Exception {
        mockMvc.perform(post("/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- validation and error contract

    @Test
    void invalidPayloadsAreRejectedWithFieldMessagesInsteadOfServerErrors() throws Exception {
        String token = adminToken();
        long establishmentId = createEstablishment(token, 10);

        mockMvc.perform(post("/clients").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"name\":\"A\",\"cpf\":\"111.111.111-11\",\"establishmentId\":" + establishmentId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cpf")));
        mockMvc.perform(post("/clients").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"cpf\":\"" + newCpf() + "\",\"establishmentId\":" + establishmentId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
        mockMvc.perform(post("/establishments").header("Authorization", bearer(token)).contentType("application/json")
                        .content(establishmentJson(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("valuePerPoint")));
        mockMvc.perform(post("/establishments").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"name\":\"No CNPJ\",\"valuePerPoint\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cnpj")));
        mockMvc.perform(post("/purchases").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"clientId\":1,\"establishmentId\":" + establishmentId + ",\"amount\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("amount")));
        mockMvc.perform(post("/purchases").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{\"establishmentId\":" + establishmentId + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonAndUnsupportedMethodsUseTheApiErrorShape() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/clients").header("Authorization", bearer(token)).contentType("application/json")
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
        mockMvc.perform(delete("/clients/1").header("Authorization", bearer(token)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
        mockMvc.perform(post("/clients/1/points/adjust").contentType("application/json").content("{}")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void weakPasswordIsRejectedWhenCreatingAUser() throws Exception {
        long establishmentId = createEstablishment(adminToken(), 10);

        mockMvc.perform(post("/users").header("Authorization", bearer(adminToken())).contentType("application/json")
                        .content("{\"name\":\"Weak\",\"email\":\"weak" + SEQUENCE.incrementAndGet()
                                + "@test.com\",\"password\":\"short\",\"role\":\"ESTABLISHMENT_STAFF\",\"establishmentId\":"
                                + establishmentId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("password")));
    }

    @Test
    void duplicateCpfAndCnpjAreConflictsNotServerErrors() throws Exception {
        String token = adminToken();
        long establishmentId = createEstablishment(token, 10);
        String cpf = newCpf();

        createClient(token, establishmentId, cpf);
        mockMvc.perform(post("/clients").header("Authorization", bearer(token)).contentType("application/json")
                        .content(clientJson(cpf, establishmentId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        String cnpj = newCnpj();
        String body = "{\"name\":\"Dup\",\"cnpj\":\"" + cnpj + "\",\"valuePerPoint\":10}";
        mockMvc.perform(post("/establishments").header("Authorization", bearer(token))
                .contentType("application/json").content(body)).andExpect(status().isOk());
        mockMvc.perform(post("/establishments").header("Authorization", bearer(token))
                .contentType("application/json").content(body)).andExpect(status().isConflict());
    }

    @Test
    void missingResourcesAreNotFoundForPlatformAdmin() throws Exception {
        String token = adminToken();

        mockMvc.perform(get("/clients/999999").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mockMvc.perform(get("/purchases/999999").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mockMvc.perform(post("/clients/999999/points/adjust").contentType("application/json").content("{\"points\":1,\"reason\":\"test\"}").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/clients/999999").header("Authorization", bearer(token))
                        .contentType("application/json").content("{\"name\":\"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/purchases/999999").header("Authorization", bearer(token))
                        .contentType("application/json").content("{\"amount\":10}"))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- points integrity

    @Test
    void pointsBalanceCanNeverGoNegative() throws Exception {
        String token = adminToken();
        long establishmentId = createEstablishment(token, 10);
        long clientId = createClient(token, establishmentId, newCpf());

        mockMvc.perform(post("/clients/" + clientId + "/points/adjust").contentType("application/json").content("{\"points\":-1,\"reason\":\"test\"}")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/clients/" + clientId + "/points/adjust").contentType("application/json").content("{\"points\":20,\"reason\":\"test\"}")
                .header("Authorization", bearer(token))).andExpect(status().isOk());
        mockMvc.perform(post("/clients/" + clientId + "/points/adjust").contentType("application/json").content("{\"points\":-21,\"reason\":\"test\"}")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/clients/" + clientId + "/points/adjust").contentType("application/json").content("{\"points\":-20,\"reason\":\"test\"}")
                .header("Authorization", bearer(token))).andExpect(status().isOk());

        assertEquals(0, pointsOf(token, clientId));
    }

    @Test
    void purchaseUpdateCannotLeakPointsToAnotherEstablishmentsClient() throws Exception {
        String admin = adminToken();
        long estA = createEstablishment(admin, 10);
        long estB = createEstablishment(admin, 10);
        long clientA = createClient(admin, estA, newCpf());
        long clientB = createClient(admin, estB, newCpf());
        String ownerA = login(createUser(admin, "ESTABLISHMENT_OWNER", estA));

        mockMvc.perform(post("/purchases").header("Authorization", bearer(ownerA)).contentType("application/json")
                        .content("{\"clientId\":" + clientA + ",\"amount\":100.00}"))
                .andExpect(status().isOk());
        assertEquals(10, pointsOf(admin, clientA));
        long purchaseId = onlyPurchaseIdOf(ownerA);

        // Owner A tries to reassign the purchase to establishment B's client
        mockMvc.perform(put("/purchases/" + purchaseId).header("Authorization", bearer(ownerA))
                        .contentType("application/json").content("{\"clientId\":" + clientB + "}"))
                .andExpect(status().isNotFound());
        assertEquals(0, pointsOf(admin, clientB));
        assertEquals(10, pointsOf(admin, clientA));

        // A legitimate edit re-computes the points instead of leaving the old ones
        mockMvc.perform(put("/purchases/" + purchaseId).header("Authorization", bearer(ownerA))
                        .contentType("application/json").content("{\"amount\":200.00}"))
                .andExpect(status().isOk());
        assertEquals(20, pointsOf(admin, clientA));
    }

    @Test
    void concurrentPurchasesForTheSameClientNeverLoseOrInventPoints() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin, 10);
        long clientId = createClient(admin, establishmentId, newCpf());
        String ownerToken = login(createUser(admin, "ESTABLISHMENT_OWNER", establishmentId));
        int purchases = 12;

        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < purchases; i++) {
                results.add(pool.submit(() -> mockMvc.perform(post("/purchases")
                                .header("Authorization", bearer(ownerToken)).contentType("application/json")
                                .content("{\"clientId\":" + clientId + ",\"amount\":100.00}"))
                        .andReturn().getResponse().getStatus()));
            }
            for (Future<Integer> result : results) {
                assertEquals(200, result.get(60, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(purchases * 10, pointsOf(admin, clientId));
    }

    // ---------------------------------------------------------------- helpers

    private String adminToken() throws Exception {
        return extractToken(mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@pointsback.local\",\"password\":\"ChangeMe123!\"}"))
                .andExpect(status().isOk()).andReturn());
    }

    private String login(String email) throws Exception {
        return extractToken(mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn());
    }

    private String extractToken(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createEstablishment(String adminToken, int valuePerPoint) throws Exception {
        MvcResult result = mockMvc.perform(post("/establishments").header("Authorization", bearer(adminToken))
                        .contentType("application/json").content(establishmentJson(valuePerPoint)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createClient(String token, long establishmentId, String cpf) throws Exception {
        MvcResult result = mockMvc.perform(post("/clients").header("Authorization", bearer(token))
                        .contentType("application/json").content(clientJson(cpf, establishmentId)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Creates a user with {@link #PASSWORD}; returns its email. */
    private String createUser(String adminToken, String role, long establishmentId) throws Exception {
        String email = "user" + SEQUENCE.incrementAndGet() + "@test.com";
        mockMvc.perform(post("/users").header("Authorization", bearer(adminToken))
                        .contentType("application/json").content(userJson(email, role, establishmentId)))
                .andExpect(status().isOk());
        // A password given by a manager is temporary; these tests are not about the forced change
        jdbc.update("UPDATE users SET must_change_password = FALSE WHERE email = ?", email);
        return email;
    }

    private long userIdOf(String email) throws Exception {
        MvcResult result = mockMvc.perform(get("/users").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode user : json.readTree(result.getResponse().getContentAsString())) {
            if (email.equals(user.get("email").asText())) {
                return user.get("id").asLong();
            }
        }
        throw new AssertionError("user not found: " + email);
    }

    private long onlyPurchaseIdOf(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/purchases").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode purchases = json.readTree(result.getResponse().getContentAsString());
        assertEquals(1, purchases.size());
        return purchases.get(0).get("purchaseId").asLong();
    }

    private int pointsOf(String token, long clientId) throws Exception {
        MvcResult result = mockMvc.perform(get("/clients/" + clientId).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("points").asInt();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String establishmentJson(int valuePerPoint) {
        return "{\"name\":\"Shop " + SEQUENCE.incrementAndGet() + "\",\"email\":\"shop@test.com\",\"cnpj\":\""
                + newCnpj() + "\",\"valuePerPoint\":" + valuePerPoint + "}";
    }

    private static String clientJson(String cpf, long establishmentId) {
        return "{\"name\":\"Client\",\"email\":\"client@test.com\",\"cpf\":\"" + cpf
                + "\",\"establishmentId\":" + establishmentId + "}";
    }

    private static String userJson(String email, String role, long establishmentId) {
        return "{\"name\":\"User\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                + "\",\"role\":\"" + role + "\",\"establishmentId\":" + establishmentId + "}";
    }

    /** A unique, checksum-valid CPF (digits only). */
    private static String newCpf() {
        StringBuilder digits = new StringBuilder(String.valueOf(SEQUENCE.incrementAndGet()));
        for (int length : new int[] {9, 10}) {
            int sum = 0;
            for (int i = 0; i < length; i++) {
                sum += (digits.charAt(i) - '0') * (length + 1 - i);
            }
            int remainder = (sum * 10) % 11;
            digits.append(remainder == 10 ? 0 : remainder);
        }
        return digits.toString();
    }

    /** A unique, checksum-valid CNPJ (digits only). */
    private static String newCnpj() {
        StringBuilder digits = new StringBuilder(String.format("%08d", SEQUENCE.incrementAndGet() % 100_000_000L) + "0001");
        int[][] weights = {{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}, {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}};
        for (int[] w : weights) {
            int sum = 0;
            for (int i = 0; i < w.length; i++) {
                sum += (digits.charAt(i) - '0') * w[i];
            }
            int remainder = sum % 11;
            digits.append(remainder < 2 ? 0 : 11 - remainder);
        }
        return digits.toString();
    }
}
