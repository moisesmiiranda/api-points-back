package com.mmiranda.pointsbackapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartupSecurityGuardTest {

    private static final String STRONG_SECRET = "a-real-secret-with-more-than-32-characters!!";
    private static final String STRONG_PASSWORD = "a-long-admin-password";

    private static StartupSecurityGuard guard(String activeProfile, String secret, String adminPassword) {
        MockEnvironment environment = new MockEnvironment();
        if (activeProfile != null) {
            environment.setActiveProfiles(activeProfile);
        }
        return new StartupSecurityGuard(environment, secret, adminPassword);
    }

    @Test
    void acceptsStrongSecretsWithoutADevProfile() {
        assertDoesNotThrow(() -> guard(null, STRONG_SECRET, STRONG_PASSWORD).run(null));
    }

    @Test
    void devAndTestProfilesAreExemptSoTheyMayUseConvenientDefaults() {
        assertDoesNotThrow(() -> guard("dev", StartupSecurityGuard.DEV_JWT_SECRET,
                StartupSecurityGuard.DEV_ADMIN_PASSWORD).run(null));
        assertDoesNotThrow(() -> guard("test", "short", "short").run(null));
    }

    @Test
    void rejectsAShortJwtSecret() {
        assertThrows(IllegalStateException.class, () -> guard(null, "too-short", STRONG_PASSWORD).run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, null, STRONG_PASSWORD).run(null));
    }

    @Test
    void rejectsTheWellKnownDevJwtSecret() {
        assertThrows(IllegalStateException.class,
                () -> guard(null, StartupSecurityGuard.DEV_JWT_SECRET, STRONG_PASSWORD).run(null));
    }

    @Test
    void rejectsAWeakOrDefaultAdminPassword() {
        assertThrows(IllegalStateException.class, () -> guard(null, STRONG_SECRET, "short").run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, STRONG_SECRET, null).run(null));
        assertThrows(IllegalStateException.class,
                () -> guard(null, STRONG_SECRET, StartupSecurityGuard.DEV_ADMIN_PASSWORD).run(null));
    }
}
