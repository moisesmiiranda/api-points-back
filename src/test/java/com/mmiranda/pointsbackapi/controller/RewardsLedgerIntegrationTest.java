package com.mmiranda.pointsbackapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Points ledger, purchase discounts and cancellation, reward settings, the reward catalog and
 * redemptions, end to end on the real filter chain, method security, validation and database.
 * Every test creates its own establishment and users.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsLedgerIntegrationTest {

    private static final String PASSWORD = "Passw0rd!";
    private static final AtomicLong SEQUENCE = new AtomicLong(500_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    /** An establishment with an owner, a staff member and one client. */
    private record Shop(long id, String owner, String staff, long clientId) {
    }

    // ------------------------------------------------------------------ catalog mode

    @Test
    void catalogFlowRedeemsARewardOnceAndKeepsTheStatementConsistent() throws Exception {
        Shop shop = shop();
        long rewardId = createReward(shop.owner(), "Coffee", 50, 1);
        adjust(shop.owner(), shop.clientId(), 120, "Welcome bonus");

        // Staff can read the catalog and redeem, but not manage it, and not correct balances
        mockMvc.perform(get("/rewards").header("Authorization", bearer(shop.staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Coffee"));
        mockMvc.perform(post("/rewards").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content("{\"name\":\"Free\",\"type\":\"BRINDE\",\"pointsCost\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/clients/" + shop.clientId() + "/points/adjust").header("Authorization", bearer(shop.staff()))
                        .contentType("application/json").content("{\"points\":5,\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());

        MvcResult redeemed = mockMvc.perform(post("/redemptions").header("Authorization", bearer(shop.staff()))
                        .contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.pointsCost").value(50))
                .andReturn();
        JsonNode voucher = json.readTree(redeemed.getResponse().getContentAsString());
        assertEquals(8, voucher.get("code").asText().length());
        assertEquals(70, pointsOf(shop.owner(), shop.clientId()));

        // The only unit is gone
        mockMvc.perform(post("/redemptions").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isConflict());

        // Hand the voucher over, once
        long redemptionId = voucher.get("id").asLong();
        mockMvc.perform(post("/redemptions/" + redemptionId + "/use").header("Authorization", bearer(shop.staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
        mockMvc.perform(post("/redemptions/" + redemptionId + "/use").header("Authorization", bearer(shop.staff())))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/redemptions").param("clientId", String.valueOf(shop.clientId()))
                        .header("Authorization", bearer(shop.owner())))
                .andExpect(jsonPath("$[0].code").value(voucher.get("code").asText()));

        // The statement is newest first and its running balance adds up
        JsonNode statement = statementOf(shop.owner(), shop.clientId());
        assertEquals(2, statement.get("totalElements").asInt());
        assertEquals("REDEEM", statement.get("content").get(0).get("type").asText());
        assertEquals(70, statement.get("content").get(0).get("balanceAfter").asInt());
        assertEquals("ADJUST", statement.get("content").get(1).get("type").asText());
        assertEquals("Welcome bonus", statement.get("content").get(1).get("reason").asText());
        assertLedgerMatchesBalance(shop.owner(), shop.clientId());
    }

    @Test
    void anotherEstablishmentsOwnerCannotRedeemOrUseVouchers() throws Exception {
        Shop shop = shop();
        Shop other = shop();
        long rewardId = createReward(shop.owner(), "Coffee", 10, null);
        adjust(shop.owner(), shop.clientId(), 100, "x");

        mockMvc.perform(post("/redemptions").header("Authorization", bearer(other.owner())).contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isForbidden());
        // The same reward through the other shop's own client is not found either
        mockMvc.perform(post("/redemptions").header("Authorization", bearer(other.owner())).contentType("application/json")
                        .content("{\"clientId\":" + other.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/rewards/" + rewardId).header("Authorization", bearer(other.owner())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/clients/" + shop.clientId() + "/statement").header("Authorization", bearer(other.owner())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatedRewardsCannotBeRedeemed() throws Exception {
        Shop shop = shop();
        long rewardId = createReward(shop.owner(), "Coffee", 10, null);
        adjust(shop.owner(), shop.clientId(), 100, "x");

        mockMvc.perform(delete("/rewards/" + rewardId).header("Authorization", bearer(shop.owner())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/redemptions").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/rewards/" + rewardId).header("Authorization", bearer(shop.owner()))
                        .contentType("application/json").content("{\"active\":true,\"pointsCost\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsCost").value(20));
        mockMvc.perform(post("/redemptions").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ discount / cashback mode

    @Test
    void discountModeAppliesPointsToAPurchaseCapsThemAndCancellationRestoresEverything() throws Exception {
        Shop shop = shop();
        setSettings(shop.owner(), shop.id(), "DISCOUNT", "0.1000", 30);
        adjust(shop.owner(), shop.clientId(), 1000, "Migrated balance");

        // Catalog redemptions are switched off in this mode
        long rewardId = createReward(shop.owner(), "Coffee", 10, null);
        mockMvc.perform(post("/redemptions").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/clients/" + shop.clientId() + "/redeemable").param("amount", "100.00")
                        .header("Authorization", bearer(shop.staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000))
                .andExpect(jsonPath("$.maxPoints").value(300))
                .andExpect(jsonPath("$.maxDiscount").value(30.00));

        // Above the 30% cap
        mockMvc.perform(post("/purchases").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content(purchaseJson(shop.clientId(), "100.00", 301)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("maximum")));

        // 300 points = 30.00 off: pays 70.00, earns 7 points (value per point is 10)
        MvcResult purchase = mockMvc.perform(post("/purchases").header("Authorization", bearer(shop.staff()))
                        .contentType("application/json").content(purchaseJson(shop.clientId(), "100.00", 300)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(30.00))
                .andExpect(jsonPath("$.redeemPoints").value(300))
                .andExpect(jsonPath("$.pointsEarned").value(7))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        long purchaseId = json.readTree(purchase.getResponse().getContentAsString()).get("purchaseId").asLong();
        assertEquals(1000 - 300 + 7, pointsOf(shop.owner(), shop.clientId()));

        // Purchases paid with points cannot be edited; only owners cancel
        mockMvc.perform(put("/purchases/" + purchaseId).header("Authorization", bearer(shop.owner()))
                        .contentType("application/json").content("{\"amount\":50.00}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/purchases/" + purchaseId + "/cancel").header("Authorization", bearer(shop.staff())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/purchases/" + purchaseId + "/cancel").header("Authorization", bearer(shop.owner())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledAt").exists());

        assertEquals(1000, pointsOf(shop.owner(), shop.clientId()));
        mockMvc.perform(post("/purchases/" + purchaseId + "/cancel").header("Authorization", bearer(shop.owner())))
                .andExpect(status().isConflict());
        assertLedgerMatchesBalance(shop.owner(), shop.clientId());
    }

    @Test
    void cancellationIsRefusedWhenTheEarnedPointsWereAlreadySpent() throws Exception {
        Shop shop = shop();
        setSettings(shop.owner(), shop.id(), "CASHBACK", "0.1000", 100);
        // 100.00 earns 10 points
        long purchaseId = registerPurchase(shop, "100.00", null);
        assertEquals(10, pointsOf(shop.owner(), shop.clientId()));
        // The client spends all 10 points on a later purchase (1.00 off a 5.00 sale, earns 0)
        registerPurchase(shop, "5.00", 10);
        assertEquals(0, pointsOf(shop.owner(), shop.clientId()));

        mockMvc.perform(post("/purchases/" + purchaseId + "/cancel").header("Authorization", bearer(shop.owner())))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(0, pointsOf(shop.owner(), shop.clientId()));
        assertLedgerMatchesBalance(shop.owner(), shop.clientId());
    }

    @Test
    void editingAPurchaseWritesLedgerEntriesAndKeepsTheBalanceRight() throws Exception {
        Shop shop = shop();
        long purchaseId = registerPurchase(shop, "100.00", null);
        assertEquals(10, pointsOf(shop.owner(), shop.clientId()));

        mockMvc.perform(put("/purchases/" + purchaseId).header("Authorization", bearer(shop.staff()))
                        .contentType("application/json").content("{\"amount\":250.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsEarned").value(25));

        assertEquals(25, pointsOf(shop.owner(), shop.clientId()));
        assertLedgerMatchesBalance(shop.owner(), shop.clientId());
    }

    @Test
    void redeemingPointsIsRefusedInCatalogModeAndBeyondTheBalance() throws Exception {
        Shop shop = shop();
        mockMvc.perform(post("/purchases").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content(purchaseJson(shop.clientId(), "100.00", 10)))
                .andExpect(status().isConflict());

        setSettings(shop.owner(), shop.id(), "DISCOUNT", "0.1000", 50);
        mockMvc.perform(post("/purchases").header("Authorization", bearer(shop.staff())).contentType("application/json")
                        .content(purchaseJson(shop.clientId(), "100.00", 10)))
                .andExpect(status().isUnprocessableEntity());
        assertEquals(0, pointsOf(shop.owner(), shop.clientId()));
    }

    // ------------------------------------------------------------------ settings, validation and legacy paths

    @Test
    void rewardSettingsAreValidatedAndOnlyOwnersChangeThem() throws Exception {
        Shop shop = shop();

        mockMvc.perform(get("/establishments/" + shop.id() + "/reward-settings").header("Authorization", bearer(shop.staff())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardMode").value("CATALOG"));
        mockMvc.perform(put("/establishments/" + shop.id() + "/reward-settings").header("Authorization", bearer(shop.staff()))
                        .contentType("application/json").content("{\"rewardMode\":\"DISCOUNT\"}"))
                .andExpect(status().isForbidden());
        for (String invalid : new String[] {
                "{\"rewardMode\":\"DISCOUNT\",\"pointsToCurrencyRate\":0}",
                "{\"rewardMode\":\"DISCOUNT\",\"maxDiscountPercent\":101}",
                "{\"rewardMode\":\"DISCOUNT\",\"maxDiscountPercent\":0}",
                "{\"rewardMode\":\"NOPE\"}",
                "{}"}) {
            mockMvc.perform(put("/establishments/" + shop.id() + "/reward-settings").header("Authorization", bearer(shop.owner()))
                            .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rewardsAreValidated() throws Exception {
        Shop shop = shop();
        for (String invalid : new String[] {
                "{\"type\":\"BRINDE\",\"pointsCost\":10}",
                "{\"name\":\"x\",\"pointsCost\":10}",
                "{\"name\":\"x\",\"type\":\"BRINDE\",\"pointsCost\":0}",
                "{\"name\":\"x\",\"type\":\"BRINDE\",\"pointsCost\":10,\"stock\":-1}"}) {
            mockMvc.perform(post("/rewards").header("Authorization", bearer(shop.owner()))
                            .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void pointsCanOnlyChangeThroughTheLedgerPaths() throws Exception {
        Shop shop = shop();
        adjust(shop.owner(), shop.clientId(), 40, "x");

        // A client created with a balance starts at zero
        MvcResult created = mockMvc.perform(post("/clients").header("Authorization", bearer(shop.owner()))
                        .contentType("application/json")
                        .content("{\"name\":\"Rich\",\"cpf\":\"" + newCpf() + "\",\"points\":9999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(0)).andReturn();
        assertNotNull(json.readTree(created.getResponse().getContentAsString()).get("id"));

        // Editing a client cannot touch the balance
        mockMvc.perform(put("/clients/" + shop.clientId()).header("Authorization", bearer(shop.owner()))
                        .contentType("application/json").content("{\"name\":\"Renamed\",\"points\":9999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points").value(40));
        // The old free-form endpoint is gone
        mockMvc.perform(put("/clients/" + shop.clientId() + "/points").param("points", "5")
                        .header("Authorization", bearer(shop.owner())))
                .andExpect(status().isNotFound());
        // A zero adjustment or one without a reason is rejected
        mockMvc.perform(post("/clients/" + shop.clientId() + "/points/adjust").header("Authorization", bearer(shop.owner()))
                        .contentType("application/json").content("{\"points\":0,\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/clients/" + shop.clientId() + "/points/adjust").header("Authorization", bearer(shop.owner()))
                        .contentType("application/json").content("{\"points\":5}"))
                .andExpect(status().isBadRequest());
        assertEquals(40, pointsOf(shop.owner(), shop.clientId()));
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    void concurrentRedemptionsNeverOversellTheStockOrOverspendTheBalance() throws Exception {
        Shop shop = shop();
        long limited = createReward(shop.owner(), "Limited", 10, 3);
        adjust(shop.owner(), shop.clientId(), 1000, "x");

        List<Integer> stockResults = runConcurrently(8, () -> redeem(shop, limited));
        assertEquals(3, stockResults.stream().filter(code -> code == 200).count());
        assertEquals(5, stockResults.stream().filter(code -> code == 409).count());
        assertEquals(1000 - 3 * 10, pointsOf(shop.owner(), shop.clientId()));

        // Balance of 100 buys at most two rewards costing 50 each, however many requests race
        Shop poor = shop();
        long unlimited = createReward(poor.owner(), "Unlimited", 50, null);
        adjust(poor.owner(), poor.clientId(), 100, "x");
        List<Integer> balanceResults = runConcurrently(6, () -> redeem(poor, unlimited));
        assertEquals(2, balanceResults.stream().filter(code -> code == 200).count());
        assertEquals(4, balanceResults.stream().filter(code -> code == 422).count());
        assertEquals(0, pointsOf(poor.owner(), poor.clientId()));
        assertLedgerMatchesBalance(poor.owner(), poor.clientId());
    }

    // ------------------------------------------------------------------ helpers

    private List<Integer> runConcurrently(int requests, java.util.concurrent.Callable<Integer> call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(pool.submit(call));
            }
            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private int redeem(Shop shop, long rewardId) throws Exception {
        return mockMvc.perform(post("/redemptions").header("Authorization", bearer(shop.staff()))
                        .contentType("application/json")
                        .content("{\"clientId\":" + shop.clientId() + ",\"rewardId\":" + rewardId + "}"))
                .andReturn().getResponse().getStatus();
    }

    /** The statement's points always add up to the balance, and each row's balanceAfter is the running total. */
    private void assertLedgerMatchesBalance(String token, long clientId) throws Exception {
        MvcResult result = mockMvc.perform(get("/clients/" + clientId + "/statement").param("size", "100")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        JsonNode rows = json.readTree(result.getResponse().getContentAsString()).get("content");
        int sum = 0;
        for (int i = rows.size() - 1; i >= 0; i--) { // oldest first
            sum += rows.get(i).get("points").asInt();
            assertEquals(sum, rows.get(i).get("balanceAfter").asInt());
        }
        assertEquals(pointsOf(token, clientId), sum);
    }

    private long registerPurchase(Shop shop, String amount, Integer redeemPoints) throws Exception {
        MvcResult result = mockMvc.perform(post("/purchases").header("Authorization", bearer(shop.staff()))
                        .contentType("application/json").content(purchaseJson(shop.clientId(), amount, redeemPoints)))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("purchaseId").asLong();
    }

    private static String purchaseJson(long clientId, String amount, Integer redeemPoints) {
        return "{\"clientId\":" + clientId + ",\"amount\":" + amount
                + (redeemPoints != null ? ",\"redeemPoints\":" + redeemPoints : "") + "}";
    }

    private void setSettings(String ownerToken, long establishmentId, String mode, String rate, int maxPercent) throws Exception {
        mockMvc.perform(put("/establishments/" + establishmentId + "/reward-settings")
                        .header("Authorization", bearer(ownerToken)).contentType("application/json")
                        .content("{\"rewardMode\":\"" + mode + "\",\"pointsToCurrencyRate\":" + rate
                                + ",\"maxDiscountPercent\":" + maxPercent + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardMode").value(mode));
    }

    private void adjust(String ownerToken, long clientId, int points, String reason) throws Exception {
        mockMvc.perform(post("/clients/" + clientId + "/points/adjust").header("Authorization", bearer(ownerToken))
                        .contentType("application/json")
                        .content("{\"points\":" + points + ",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    private long createReward(String ownerToken, String name, int cost, Integer stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/rewards").header("Authorization", bearer(ownerToken))
                        .contentType("application/json")
                        .content("{\"name\":\"" + name + "\",\"type\":\"BRINDE\",\"pointsCost\":" + cost
                                + (stock != null ? ",\"stock\":" + stock : "") + "}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private int pointsOf(String token, long clientId) throws Exception {
        return json.readTree(mockMvc.perform(get("/clients/" + clientId).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("points").asInt();
    }

    private JsonNode statementOf(String token, long clientId) throws Exception {
        return json.readTree(mockMvc.perform(get("/clients/" + clientId + "/statement").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private Shop shop() throws Exception {
        String admin = login("admin@pointsback.local", "ChangeMe123!");
        long id = json.readTree(mockMvc.perform(post("/establishments").header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Shop " + SEQUENCE.incrementAndGet() + "\",\"cnpj\":\"" + newCnpj()
                                + "\",\"valuePerPoint\":10}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        String owner = login(createUser(admin, "ESTABLISHMENT_OWNER", id), PASSWORD);
        String staff = login(createUser(admin, "ESTABLISHMENT_STAFF", id), PASSWORD);
        long clientId = json.readTree(mockMvc.perform(post("/clients").header("Authorization", bearer(owner))
                        .contentType("application/json")
                        .content("{\"name\":\"Client\",\"cpf\":\"" + newCpf() + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        return new Shop(id, owner, staff, clientId);
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

    private String login(String email, String password) throws Exception {
        MockHttpServletRequestBuilder request = post("/auth/login").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
        return json.readTree(mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString()).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
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
        StringBuilder digits = new StringBuilder(String.format("%08d", 30_000_000L + SEQUENCE.incrementAndGet() % 1_000_000L) + "0001");
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
