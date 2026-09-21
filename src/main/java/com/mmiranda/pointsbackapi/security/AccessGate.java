package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.User;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Decides, for an authenticated request, whether the account may go on:
 * <ul>
 *   <li>Its establishment is suspended (or the trial ended): everything is refused with 402, except reading
 *       its own profile and plan, so the app can explain what happened.</li>
 *   <li>Its password was set by someone else (must_change_password): everything is refused with 403 until
 *       it is replaced, except reading its own profile and changing the password.</li>
 * </ul>
 * PLATFORM_ADMIN has no establishment and is never blocked.
 */
public final class AccessGate {

    public record Block(HttpStatus status, String error, String message) {
    }

    public static final String SUSPENDED_MESSAGE =
            "The establishment's access is suspended. Contact support to regularize it.";
    public static final String PASSWORD_CHANGE_MESSAGE =
            "You must change your temporary password before continuing.";

    private AccessGate() {
    }

    public static Optional<Block> check(User account, String method, String path, LocalDate today) {
        Establishment establishment = account.getEstablishment();
        if (establishment != null && establishment.isBlocked(today)) {
            boolean allowed = "GET".equals(method)
                    && (path.equals("/users/me") || path.equals("/establishments/" + establishment.getId() + "/plan"));
            if (!allowed) {
                return Optional.of(new Block(HttpStatus.PAYMENT_REQUIRED, "Payment Required", SUSPENDED_MESSAGE));
            }
        }
        if (account.isMustChangePassword()) {
            boolean allowed = ("GET".equals(method) && path.equals("/users/me"))
                    || ("POST".equals(method) && path.equals("/users/me/password"));
            if (!allowed) {
                return Optional.of(new Block(HttpStatus.FORBIDDEN, "Forbidden", PASSWORD_CHANGE_MESSAGE));
            }
        }
        return Optional.empty();
    }
}
