package com.mmiranda.pointsbackapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartupMailGuardTest {

    private static StartupMailGuard guard(String profile, String provider, String host, String from, String baseUrl) {
        MockEnvironment environment = new MockEnvironment();
        if (profile != null) {
            environment.setActiveProfiles(profile);
        }
        return new StartupMailGuard(environment, provider, host, from, baseUrl);
    }

    @Test
    void acceptsARealSmtpSetup() {
        assertDoesNotThrow(() -> guard(null, "smtp", "smtp.provedor.com", "no-reply@loja.com.br", "https://app.loja.com.br").run(null));
    }

    @Test
    void devAndTestProfilesMayLogEmailsInsteadOfSendingThem() {
        assertDoesNotThrow(() -> guard("dev", "log", "", "", "").run(null));
        assertDoesNotThrow(() -> guard("test", "log", "${MAIL_HOST}", "", "").run(null));
    }

    @Test
    void productionRefusesTheLogSenderBecauseItWritesLiveLinksToTheLog() {
        assertThrows(IllegalStateException.class,
                () -> guard(null, "log", "smtp.x.com", "a@b.com", "https://app.x.com").run(null));
    }

    @Test
    void productionRequiresHostSenderAndFrontendUrl() {
        assertThrows(IllegalStateException.class, () -> guard(null, "smtp", "", "a@b.com", "https://app.x.com").run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, "smtp", "${MAIL_HOST}", "a@b.com", "https://app.x.com").run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, "smtp", "smtp.x.com", " ", "https://app.x.com").run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, "smtp", "smtp.x.com", "a@b.com", null).run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, "smtp", "smtp.x.com", "a@b.com", "${APP_BASE_URL}").run(null));
        assertThrows(IllegalStateException.class, () -> guard(null, "smtp", "smtp.x.com", "a@b.com", "app.x.com").run(null));
    }
}
