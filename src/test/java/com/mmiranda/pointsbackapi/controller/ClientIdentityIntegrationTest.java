package com.mmiranda.pointsbackapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The CPF identifies a person, who may be a client of several establishments, each with its own
 * balance and contact data; establishments in a group may opt in to sharing clients.
 * Every test creates its own establishments and users, so it is independent of the seeded data.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClientIdentityIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";
    private static final AtomicLong SEQUENCE = new AtomicLong(800_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void samePersonIsAClientOfTwoEstablishmentsWithIndependentBalancesAndData() throws Exception {
        String admin = adminToken();
        long estA = createEstablishment(admin);
        long estB = createEstablishment(admin);
        String ownerA = login(createUser(admin, "ESTABLISHMENT_OWNER", estA));
        String ownerB = login(createUser(admin, "ESTABLISHMENT_OWNER", estB));
        String cpf = newCpf();

        long accountA = createClient(ownerA, "Ana at A", "(11) 99999-8888", formatCpf(cpf));
        long accountB = createClient(ownerB, "Ana at B", "21 88888-7777", cpf);
        assertNotEquals(accountA, accountB);

        mockMvc.perform(post("/clients/" + accountA + "/points/adjust").contentType("application/json").content("{\"points\":50,\"reason\":\"test\"}")
                .header("Authorization", bearer(ownerA))).andExpect(status().isOk());

        assertEquals(50, clientOf(ownerA, accountA).get("points").asInt());
        assertEquals(0, clientOf(ownerB, accountB).get("points").asInt());
        assertEquals("Ana at A", clientOf(ownerA, accountA).get("name").asText());
        assertEquals("Ana at B", clientOf(ownerB, accountB).get("name").asText());
        assertEquals(formatCpf(cpf), clientOf(ownerB, accountB).get("cpf").asText());

        // B never sees A's account or its data
        mockMvc.perform(get("/clients/" + accountA).header("Authorization", bearer(ownerB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/clients/all").header("Authorization", bearer(ownerB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(accountB));
    }

    @Test
    void aPersonCannotBeRegisteredTwiceInTheSameEstablishmentEvenWithDifferentCpfFormatting() throws Exception {
        String admin = adminToken();
        long est = createEstablishment(admin);
        String owner = login(createUser(admin, "ESTABLISHMENT_OWNER", est));
        String cpf = newCpf();

        createClient(owner, "First", "11999990000", cpf);

        mockMvc.perform(post("/clients").header("Authorization", bearer(owner)).contentType("application/json")
                        .content(clientJson("Second", "11999991111", formatCpf(cpf))))
                .andExpect(status().isConflict());
    }

    @Test
    void searchFindsByCpfOrPhoneOnlyInsideTheCallersEstablishment() throws Exception {
        String admin = adminToken();
        long estA = createEstablishment(admin);
        long estB = createEstablishment(admin);
        String ownerA = login(createUser(admin, "ESTABLISHMENT_OWNER", estA));
        String ownerB = login(createUser(admin, "ESTABLISHMENT_OWNER", estB));
        String cpf = newCpf();
        long accountA = createClient(ownerA, "Ana at A", "(11) 99999-8888", cpf);
        long accountB = createClient(ownerB, "Ana at B", "21 88888-7777", cpf);

        mockMvc.perform(get("/clients/search").param("cpf", formatCpf(cpf)).header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(accountA));
        mockMvc.perform(get("/clients/search").param("cpf", cpf).header("Authorization", bearer(ownerB)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(accountB));
        mockMvc.perform(get("/clients/search").param("phone", "11 99999 8888").header("Authorization", bearer(ownerA)))
                .andExpect(jsonPath("$[0].id").value(accountA));
        // A's phone does not exist in B's establishment
        mockMvc.perform(get("/clients/search").param("phone", "11999998888").header("Authorization", bearer(ownerB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        // The admin sees both accounts of the person
        mockMvc.perform(get("/clients/search").param("cpf", cpf).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/clients/search").header("Authorization", bearer(ownerA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientsAreSharedInsideAGroupOnlyWhenBothSidesOptIn() throws Exception {
        String admin = adminToken();
        long estA = createEstablishment(admin);
        long estB = createEstablishment(admin);
        String ownerA = login(createUser(admin, "ESTABLISHMENT_OWNER", estA));
        String ownerB = login(createUser(admin, "ESTABLISHMENT_OWNER", estB));
        String staffB = login(createUser(admin, "ESTABLISHMENT_STAFF", estB));
        String cpf = newCpf();
        long accountA = createClient(ownerA, "Ana at A", "11999998888", cpf);
        mockMvc.perform(post("/clients/" + accountA + "/points/adjust").contentType("application/json").content("{\"points\":70,\"reason\":\"test\"}")
                .header("Authorization", bearer(ownerA))).andExpect(status().isOk());

        // No group yet: nothing to import from, and sharing cannot be switched on
        mockMvc.perform(post("/clients/import").header("Authorization", bearer(ownerB)).contentType("application/json")
                        .content("{\"cpf\":\"" + cpf + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/establishments/" + estB + "/sharing").header("Authorization", bearer(ownerB))
                        .contentType("application/json").content("{\"shareClients\":true}"))
                .andExpect(status().isBadRequest());

        // Only the platform admin manages groups
        mockMvc.perform(post("/establishment-groups").header("Authorization", bearer(ownerA))
                        .contentType("application/json").content("{\"name\":\"Chain\"}"))
                .andExpect(status().isForbidden());
        long groupId = json.readTree(mockMvc.perform(post("/establishment-groups")
                        .header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"name\":\"Chain " + SEQUENCE.incrementAndGet() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        for (long est : new long[] {estA, estB}) {
            mockMvc.perform(put("/establishments/" + est + "/group").header("Authorization", bearer(admin))
                            .contentType("application/json").content("{\"groupId\":" + groupId + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groupId").value(groupId))
                    .andExpect(jsonPath("$.shareClients").value(false));
        }
        mockMvc.perform(put("/establishments/" + estB + "/group").header("Authorization", bearer(ownerB))
                        .contentType("application/json").content("{\"groupId\":null}"))
                .andExpect(status().isForbidden());

        // Staff cannot opt the establishment in; its owner can
        mockMvc.perform(put("/establishments/" + estB + "/sharing").header("Authorization", bearer(staffB))
                        .contentType("application/json").content("{\"shareClients\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/establishments/" + estB + "/sharing").header("Authorization", bearer(ownerB))
                        .contentType("application/json").content("{\"shareClients\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareClients").value(true));

        // B opted in but A did not: A's client stays invisible (same 404 as an unknown CPF)
        mockMvc.perform(post("/clients/import").header("Authorization", bearer(ownerB)).contentType("application/json")
                        .content("{\"cpf\":\"" + cpf + "\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/establishments/" + estA + "/sharing").header("Authorization", bearer(ownerA))
                        .contentType("application/json").content("{\"shareClients\":true}"))
                .andExpect(status().isOk());

        // Both opted in: B gets its own account with A's contact data but a zero balance
        mockMvc.perform(post("/clients/import").header("Authorization", bearer(staffB)).contentType("application/json")
                        .content("{\"cpf\":\"" + formatCpf(cpf) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana at A"))
                .andExpect(jsonPath("$.points").value(0))
                .andExpect(jsonPath("$.establishmentId").value(estB));
        assertEquals(70, clientOf(ownerA, accountA).get("points").asInt());

        // A second import is a conflict, and another group's owner cannot import into a foreign establishment
        mockMvc.perform(post("/clients/import").header("Authorization", bearer(ownerB)).contentType("application/json")
                        .content("{\"cpf\":\"" + cpf + "\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/clients/import").header("Authorization", bearer(ownerA)).contentType("application/json")
                        .content("{\"cpf\":\"" + cpf + "\",\"establishmentId\":" + estB + "}"))
                .andExpect(status().isForbidden());

        // Leaving the group stops the establishment sharing
        mockMvc.perform(put("/establishments/" + estA + "/group").header("Authorization", bearer(admin))
                        .contentType("application/json").content("{\"groupId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareClients").value(false));
        mockMvc.perform(get("/establishments/" + estA + "/sharing").header("Authorization", bearer(ownerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").doesNotExist());
        mockMvc.perform(get("/establishments/" + estA + "/sharing").header("Authorization", bearer(ownerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void changingTheCpfRelinksTheAccountAndRefusesACpfAlreadyRegisteredThere() throws Exception {
        String admin = adminToken();
        long est = createEstablishment(admin);
        String owner = login(createUser(admin, "ESTABLISHMENT_OWNER", est));
        String cpfOne = newCpf();
        String cpfTwo = newCpf();
        long first = createClient(owner, "First", "11999990000", cpfOne);
        createClient(owner, "Second", "11999991111", cpfTwo);

        mockMvc.perform(put("/clients/" + first).header("Authorization", bearer(owner))
                        .contentType("application/json").content("{\"cpf\":\"" + cpfTwo + "\"}"))
                .andExpect(status().isConflict());

        String cpfThree = newCpf();
        mockMvc.perform(put("/clients/" + first).header("Authorization", bearer(owner))
                        .contentType("application/json").content("{\"cpf\":\"" + formatCpf(cpfThree) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value(formatCpf(cpfThree)));
        // the same CPF as before the change is free again
        createClient(owner, "Returning", "11999992222", cpfOne);
    }

    // ---------------------------------------------------------------- helpers

    private String adminToken() throws Exception {
        return tokenOf(mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"admin@pointsback.local\",\"password\":\"ChangeMe123!\"}"))
                .andExpect(status().isOk()).andReturn());
    }

    private String login(String email) throws Exception {
        return tokenOf(mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn());
    }

    private String tokenOf(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private long createEstablishment(String admin) throws Exception {
        MvcResult result = mockMvc.perform(post("/establishments").header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Shop " + SEQUENCE.incrementAndGet() + "\",\"cnpj\":\"" + newCnpj()
                                + "\",\"valuePerPoint\":10}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String createUser(String admin, String role, long establishmentId) throws Exception {
        String email = "user" + SEQUENCE.incrementAndGet() + "@test.com";
        mockMvc.perform(post("/users").header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"name\":\"User\",\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                                + "\",\"role\":\"" + role + "\",\"establishmentId\":" + establishmentId + "}"))
                .andExpect(status().isOk());
        // A password given by a manager is temporary; these tests are not about the forced change
        jdbc.update("UPDATE users SET must_change_password = FALSE WHERE email = ?", email);
        return email;
    }

    /** Creates a client in the caller's own establishment. */
    private long createClient(String token, String name, String phone, String cpf) throws Exception {
        MvcResult result = mockMvc.perform(post("/clients").header("Authorization", bearer(token))
                        .contentType("application/json").content(clientJson(name, phone, cpf)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode clientOf(String token, long id) throws Exception {
        MvcResult result = mockMvc.perform(get("/clients/" + id).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }

    private static String clientJson(String name, String phone, String cpf) {
        return "{\"name\":\"" + name + "\",\"phone\":\"" + phone + "\",\"cpf\":\"" + cpf + "\"}";
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String formatCpf(String digits) {
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-" + digits.substring(9);
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
        StringBuilder digits = new StringBuilder(String.format("%08d", 60_000_000L + SEQUENCE.incrementAndGet() % 1_000_000L) + "0001");
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
