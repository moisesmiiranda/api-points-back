package com.mmiranda.pointsbackapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmiranda.pointsbackapi.mail.EmailMessage;
import com.mmiranda.pointsbackapi.mail.LoggingEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password recovery, invitations, forced password change and establishment suspension, end to end
 * (real filter chain, database and emails captured by the dev "log" sender).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountLifecycleIntegrationTest {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
    private static final AtomicLong SEQUENCE = new AtomicLong(600_000_000L);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private LoggingEmailSender emails;

    private final ObjectMapper json = new ObjectMapper();

    // ------------------------------------------------------------------ forgot / reset

    @Test
    void forgotPasswordEmailsALinkThatSetsANewPasswordOnceAndEndsOldSessions() throws Exception {
        String email = newAccount("ESTABLISHMENT_OWNER", "Senha-Antiga1", true);
        String oldToken = tokenOf(login(email, "Senha-Antiga1"));

        forgot(email).andExpect(status().isOk());
        String link = awaitLink(email).text();
        String resetToken = tokenIn(link);
        assertTrue(link.contains("http://localhost:5173/redefinir-senha?token="));

        Thread.sleep(1100); // sessions are compared at one-second precision
        reset(resetToken, "Senha-Nova-123").andExpect(status().isOk());

        // the new password works, the old one does not, and the link cannot be used again
        loginRaw(email, "Senha-Nova-123").andExpect(status().isOk());
        loginRaw(email, "Senha-Antiga1").andExpect(status().isUnauthorized());
        reset(resetToken, "Outra-Senha-456").andExpect(status().isBadRequest());
        // a session that started before the reset is over
        mockMvc.perform(get("/users/me").header("Authorization", bearer(oldToken))).andExpect(status().isUnauthorized());
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAndNoEmail() throws Exception {
        String unknown = "ninguem" + SEQUENCE.incrementAndGet() + "@test.com";
        String known = newAccount("ESTABLISHMENT_STAFF", "Senha-Antiga1", true);

        String forUnknown = forgot(unknown).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String forKnown = forgot(known).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(forUnknown, forKnown, "the response must not reveal whether the email is registered");
        awaitLink(known);
        Thread.sleep(300);
        assertTrue(emailsTo(unknown).isEmpty());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").contentType("application/json").content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/forgot-password").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        reset("qualquer", "curta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("newPassword")));
        reset("token-que-nao-existe", "Senha-Nova-123").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This link is invalid or has expired"));
    }

    @Test
    void requestsAreThrottledPerEmailWhetherOrNotItExists() throws Exception {
        String email = "spam" + SEQUENCE.incrementAndGet() + "@test.com";

        for (int i = 0; i < 5; i++) {
            forgot(email).andExpect(status().isOk());
        }
        forgot(email).andExpect(status().isTooManyRequests());
    }

    @Test
    void anExpiredLinkIsRejected() throws Exception {
        String email = newAccount("ESTABLISHMENT_OWNER", "Senha-Antiga1", true);
        forgot(email).andExpect(status().isOk());
        String token = tokenIn(awaitLink(email).text());

        jdbc.update("UPDATE password_reset_token SET expires_at = ? WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusMinutes(1)), email);

        reset(token, "Senha-Nova-123").andExpect(status().isBadRequest());
        loginRaw(email, "Senha-Antiga1").andExpect(status().isOk());
    }

    @Test
    void onlyTheNewestLinkWorksAndDeactivatedUsersGetNone() throws Exception {
        String email = newAccount("ESTABLISHMENT_OWNER", "Senha-Antiga1", true);
        forgot(email).andExpect(status().isOk());
        awaitLinks(email, 1);
        forgot(email).andExpect(status().isOk());
        List<EmailMessage> both = awaitLinks(email, 2);
        String first = tokenIn(both.get(0).text());
        String second = tokenIn(both.get(1).text());

        reset(first, "Senha-Nova-123").andExpect(status().isBadRequest());
        reset(second, "Senha-Nova-123").andExpect(status().isOk());

        String inactive = newAccount("ESTABLISHMENT_STAFF", "Senha-Antiga1", true);
        jdbc.update("UPDATE users SET active = FALSE WHERE email = ?", inactive);
        forgot(inactive).andExpect(status().isOk());
        Thread.sleep(300);
        assertTrue(emailsTo(inactive).stream().noneMatch(m -> m.subject().startsWith("Redefinição")));
    }

    // ------------------------------------------------------------------ temporary passwords and invitations

    @Test
    void aTemporaryPasswordMustBeReplacedBeforeAnythingElse() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);
        String email = "staff" + SEQUENCE.incrementAndGet() + "@test.com";
        mockMvc.perform(post("/users").header("Authorization", bearer(admin)).contentType("application/json")
                        .content(userJson(email, "Provisoria-1", "ESTABLISHMENT_STAFF", establishmentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        EmailMessage welcome = awaitEmailTo(email);
        assertTrue(welcome.text().contains("http://localhost:5173/login"));
        assertFalse(welcome.text().contains("Provisoria-1"), "the temporary password never travels by email");

        loginRaw(email, "Provisoria-1").andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
        String token = tokenOf(login(email, "Provisoria-1"));

        mockMvc.perform(get("/clients/all").header("Authorization", bearer(token))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("change your temporary password")));
        mockMvc.perform(get("/users/me").header("Authorization", bearer(token))).andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        // wrong current password, same password, too short
        changePassword(token, "errada", "Senha-Nova-123").andExpect(status().isBadRequest());
        changePassword(token, "Provisoria-1", "Provisoria-1").andExpect(status().isBadRequest());
        changePassword(token, "Provisoria-1", "curta").andExpect(status().isBadRequest());

        Thread.sleep(1100);
        MvcResult changed = changePassword(token, "Provisoria-1", "Senha-Nova-123").andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false)).andReturn();
        String fresh = json.readTree(changed.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/clients/all").header("Authorization", bearer(fresh))).andExpect(status().isOk());
        mockMvc.perform(get("/clients/all").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        loginRaw(email, "Provisoria-1").andExpect(status().isUnauthorized());
        loginRaw(email, "Senha-Nova-123").andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void aNewUserWithoutAPasswordIsInvitedAndChoosesOneThroughTheLink() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);
        String email = "owner" + SEQUENCE.incrementAndGet() + "@test.com";

        mockMvc.perform(post("/users").header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"name\":\"Dona Nova\",\"email\":\"" + email + "\",\"role\":\"ESTABLISHMENT_OWNER\",\"establishmentId\":" + establishmentId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        EmailMessage invite = awaitLink(email);
        assertTrue(invite.text().contains("Dona Nova"));
        assertTrue(invite.text().contains("72 horas"));
        String token = tokenIn(invite.text());

        reset(token, "Minha-Senha-123").andExpect(status().isOk());
        String session = tokenOf(login(email, "Minha-Senha-123"));
        mockMvc.perform(get("/clients/all").header("Authorization", bearer(session))).andExpect(status().isOk());
        reset(token, "Outra-Senha-456").andExpect(status().isBadRequest());
    }

    @Test
    void aPasswordSetByAManagerOnAnExistingUserIsTemporaryToo() throws Exception {
        String admin = adminToken();
        String email = newAccount("ESTABLISHMENT_STAFF", "Senha-Antiga1", true);
        String staffToken = tokenOf(login(email, "Senha-Antiga1"));
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);

        Thread.sleep(1100);
        mockMvc.perform(put("/users/" + id).header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"password\":\"Definida-Pelo-Gerente1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mockMvc.perform(get("/users/me").header("Authorization", bearer(staffToken))).andExpect(status().isUnauthorized());
        loginRaw(email, "Definida-Pelo-Gerente1").andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    // ------------------------------------------------------------------ establishment status

    @Test
    void aSuspendedEstablishmentCannotLogInAndItsOpenSessionsAreCutOff() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);
        String email = createUserFor(admin, establishmentId, "ESTABLISHMENT_OWNER");
        String session = tokenOf(login(email, "Passw0rd!"));
        mockMvc.perform(get("/clients/all").header("Authorization", bearer(session))).andExpect(status().isOk());

        setPlan(admin, establishmentId, "SUSPENDED", null, "Básico").andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveStatus").value("SUSPENDED"));

        loginRaw(email, "Passw0rd!").andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402));
        loginRaw(email, "senha-errada").andExpect(status().isUnauthorized());
        // the session is cut off, except for what the app needs to explain what happened
        mockMvc.perform(get("/clients/all").header("Authorization", bearer(session))).andExpect(status().isPaymentRequired());
        mockMvc.perform(post("/purchases").header("Authorization", bearer(session)).contentType("application/json")
                        .content("{\"clientId\":1,\"amount\":10}")).andExpect(status().isPaymentRequired());
        mockMvc.perform(get("/users/me").header("Authorization", bearer(session))).andExpect(status().isOk());
        mockMvc.perform(get("/establishments/" + establishmentId + "/plan").header("Authorization", bearer(session)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.effectiveStatus").value("SUSPENDED"));
        // the platform admin is never blocked
        mockMvc.perform(get("/establishments/" + establishmentId).header("Authorization", bearer(admin))).andExpect(status().isOk());

        setPlan(admin, establishmentId, "ACTIVE", null, "Pro").andExpect(status().isOk());
        mockMvc.perform(get("/clients/all").header("Authorization", bearer(session))).andExpect(status().isOk());
        loginRaw(email, "Passw0rd!").andExpect(status().isOk());
    }

    @Test
    void anEndedTrialBlocksLikeASuspensionAndANewEstablishmentStartsATrial() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);

        // a new establishment is on a 14 day trial
        mockMvc.perform(get("/establishments/" + establishmentId + "/plan").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAL"))
                .andExpect(jsonPath("$.effectiveStatus").value("TRIAL"))
                .andExpect(jsonPath("$.trialDaysLeft").value(14));

        String email = createUserFor(admin, establishmentId, "ESTABLISHMENT_STAFF");
        setPlan(admin, establishmentId, "TRIAL", LocalDate.now().minusDays(1).toString(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAL"))
                .andExpect(jsonPath("$.effectiveStatus").value("SUSPENDED"));
        loginRaw(email, "Passw0rd!").andExpect(status().isPaymentRequired());

        // starting a trial without a date gives 14 more days
        setPlan(admin, establishmentId, "TRIAL", null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveStatus").value("TRIAL"))
                .andExpect(jsonPath("$.trialDaysLeft").value(14));
        loginRaw(email, "Passw0rd!").andExpect(status().isOk());
    }

    @Test
    void onlyThePlatformAdminChangesThePlanAndOwnersSeeOnlyTheirOwn() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);
        long otherId = createEstablishment(admin);
        String owner = tokenOf(login(createUserFor(admin, establishmentId, "ESTABLISHMENT_OWNER"), "Passw0rd!"));

        setPlan(owner, establishmentId, "ACTIVE", null, null).andExpect(status().isForbidden());
        mockMvc.perform(get("/establishments/" + establishmentId + "/plan").header("Authorization", bearer(owner))).andExpect(status().isOk());
        mockMvc.perform(get("/establishments/" + otherId + "/plan").header("Authorization", bearer(owner))).andExpect(status().isForbidden());
        setPlan(admin, establishmentId, null, null, null).andExpect(status().isBadRequest());
        mockMvc.perform(put("/establishments/999999/plan").header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"status\":\"ACTIVE\"}")).andExpect(status().isNotFound());
    }

    @Test
    void theEstablishmentListShowsTheStatusForTheAdminScreen() throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);
        setPlan(admin, establishmentId, "SUSPENDED", null, "Básico").andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/establishments/all").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn();
        JsonNode mine = null;
        for (JsonNode node : json.readTree(result.getResponse().getContentAsString())) {
            if (node.get("id").asLong() == establishmentId) {
                mine = node;
            }
        }
        assertEquals("SUSPENDED", mine.get("status").asText());
        assertEquals("SUSPENDED", mine.get("effectiveStatus").asText());
        assertEquals("Básico", mine.get("plan").asText());
    }

    // ------------------------------------------------------------------ helpers

    private org.springframework.test.web.servlet.ResultActions forgot(String email) throws Exception {
        return mockMvc.perform(post("/auth/forgot-password").contentType("application/json")
                .content("{\"email\":\"" + email + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions reset(String token, String password) throws Exception {
        return mockMvc.perform(post("/auth/reset-password").contentType("application/json")
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions changePassword(String token, String current, String next) throws Exception {
        return mockMvc.perform(post("/users/me/password").header("Authorization", bearer(token)).contentType("application/json")
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions setPlan(String token, long establishmentId, String status,
                                                                        String trialEndsAt, String plan) throws Exception {
        StringBuilder body = new StringBuilder("{");
        if (status != null) body.append("\"status\":\"").append(status).append("\",");
        if (trialEndsAt != null) body.append("\"trialEndsAt\":\"").append(trialEndsAt).append("\",");
        if (plan != null) body.append("\"plan\":\"").append(plan).append("\",");
        if (body.length() > 1) body.setLength(body.length() - 1);
        body.append("}");
        return mockMvc.perform(put("/establishments/" + establishmentId + "/plan").header("Authorization", bearer(token))
                .contentType("application/json").content(body.toString()));
    }

    private org.springframework.test.web.servlet.ResultActions loginRaw(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private JsonNode login(String email, String password) throws Exception {
        return json.readTree(loginRaw(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static String tokenOf(JsonNode login) {
        return login.get("accessToken").asText();
    }

    private String adminToken() throws Exception {
        return tokenOf(login("admin@pointsback.local", "ChangeMe123!"));
    }

    private long createEstablishment(String admin) throws Exception {
        MvcResult result = mockMvc.perform(post("/establishments").header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Shop " + SEQUENCE.incrementAndGet() + "\",\"cnpj\":\"" + newCnpj() + "\",\"valuePerPoint\":10}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    /** A user created by the admin with a password, then marked as not needing the forced change. */
    private String createUserFor(String admin, long establishmentId, String role) throws Exception {
        String email = "user" + SEQUENCE.incrementAndGet() + "@test.com";
        mockMvc.perform(post("/users").header("Authorization", bearer(admin)).contentType("application/json")
                        .content(userJson(email, "Passw0rd!", role, establishmentId)))
                .andExpect(status().isOk());
        jdbc.update("UPDATE users SET must_change_password = FALSE WHERE email = ?", email);
        return email;
    }

    private String newAccount(String role, String password, boolean settled) throws Exception {
        String admin = adminToken();
        long establishmentId = createEstablishment(admin);
        String email = "user" + SEQUENCE.incrementAndGet() + "@test.com";
        mockMvc.perform(post("/users").header("Authorization", bearer(admin)).contentType("application/json")
                        .content(userJson(email, password, role, establishmentId)))
                .andExpect(status().isOk());
        if (settled) {
            jdbc.update("UPDATE users SET must_change_password = FALSE WHERE email = ?", email);
        }
        return email;
    }

    private static String userJson(String email, String password, String role, long establishmentId) {
        return "{\"name\":\"Pessoa\",\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"role\":\"" + role
                + "\",\"establishmentId\":" + establishmentId + "}";
    }

    private List<EmailMessage> emailsTo(String email) {
        return emails.recentMessages().stream().filter(m -> m.to().equals(email)).toList();
    }

    /** Emails that carry a set-password link (reset or invitation), oldest first. */
    private List<EmailMessage> linkEmailsTo(String email) {
        return emailsTo(email).stream().filter(m -> m.text().contains("token=")).toList();
    }

    /** Emails are sent asynchronously, so wait for the first one to arrive. */
    private EmailMessage awaitEmailTo(String email) {
        await().atMost(Duration.ofSeconds(10)).until(() -> !emailsTo(email).isEmpty());
        return emailsTo(email).get(0);
    }

    private EmailMessage awaitLink(String email) {
        return awaitLinks(email, 1).get(0);
    }

    private List<EmailMessage> awaitLinks(String email, int count) {
        await().atMost(Duration.ofSeconds(10)).until(() -> linkEmailsTo(email).size() >= count);
        return linkEmailsTo(email);
    }

    private static String tokenIn(String text) {
        Matcher matcher = TOKEN.matcher(text);
        assertTrue(matcher.find(), "no link in: " + text);
        return matcher.group(1);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** A unique, checksum-valid CNPJ (digits only). */
    private static String newCnpj() {
        StringBuilder digits = new StringBuilder(String.format("%08d", 20_000_000L + SEQUENCE.incrementAndGet() % 1_000_000L) + "0001");
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
