package com.mmiranda.pointsbackapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Outside dev/test the app must be able to really deliver email (password recovery and invitations depend on
 * it) and must know the public URL of the frontend that the links point to. The log-only sender writes live
 * links to the log, so it is refused too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupMailGuard implements ApplicationRunner {

    private final Environment environment;
    private final String provider;
    private final String host;
    private final String from;
    private final String baseUrl;

    public StartupMailGuard(Environment environment,
                             @Value("${app.mail.provider}") String provider,
                             @Value("${spring.mail.host:}") String host,
                             @Value("${app.mail.from:}") String from,
                             @Value("${app.base-url:}") String baseUrl) {
        this.environment = environment;
        this.provider = provider;
        this.host = host;
        this.from = from;
        this.baseUrl = baseUrl;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            return;
        }
        if (!"smtp".equals(provider)) {
            throw new IllegalStateException("MAIL_PROVIDER must be 'smtp' outside the dev profile");
        }
        if (isMissing(host)) {
            throw new IllegalStateException("MAIL_HOST is required outside the dev profile");
        }
        if (isMissing(from)) {
            throw new IllegalStateException("MAIL_FROM is required outside the dev profile");
        }
        if (isMissing(baseUrl) || !baseUrl.matches("https?://.+")) {
            throw new IllegalStateException("APP_BASE_URL (the frontend's public URL, http:// or https://) is required outside the dev profile");
        }
    }

    /** Blank, or an unresolved ${PLACEHOLDER} that the binder left in place. */
    private static boolean isMissing(String value) {
        return value == null || value.isBlank() || value.startsWith("${");
    }
}
