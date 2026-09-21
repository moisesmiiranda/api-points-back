package com.mmiranda.pointsbackapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Fails fast at startup when a non-dev environment runs with weak or well-known secrets.
 * The dev profile is exempt because it intentionally ships convenient defaults.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupSecurityGuard implements ApplicationRunner {

    static final String DEV_JWT_SECRET = "dev-only-insecure-secret-key-please-override-in-prod-1234567890";
    static final String DEV_ADMIN_PASSWORD = "ChangeMe123!";
    static final int MIN_JWT_SECRET_BYTES = 32;
    static final int MIN_ADMIN_PASSWORD_LENGTH = 12;

    private final Environment environment;
    private final String jwtSecret;
    private final String adminPassword;

    public StartupSecurityGuard(Environment environment,
                                 @Value("${jwt.secret}") String jwtSecret,
                                 @Value("${admin.bootstrap.password}") String adminPassword) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
            return;
        }
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_JWT_SECRET_BYTES + " characters outside the dev profile");
        }
        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET is set to the well-known dev value; provide a real secret");
        }
        if (adminPassword == null || adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH
                || DEV_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least " + MIN_ADMIN_PASSWORD_LENGTH
                    + " characters and not the default outside the dev profile");
        }
    }
}
