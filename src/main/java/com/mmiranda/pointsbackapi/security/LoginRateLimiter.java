package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.exception.TooManyLoginAttemptsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory throttle for failed logins. A pair (client IP, email) is blocked after
 * {@code maxAttempts} failures inside the window; an email alone is blocked after
 * {@code maxAttempts * EMAIL_MULTIPLIER} failures, which stops a distributed guess against one
 * account without letting a single attacker lock the owner out that easily.
 * State lives in this JVM only, which is enough for a single-instance deployment.
 */
@Component
@Primary
public class LoginRateLimiter {

    static final int EMAIL_MULTIPLIER = 4;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(@Value("${security.login-rate-limit.max-attempts:5}") int maxAttempts,
                             @Value("${security.login-rate-limit.window-minutes:15}") long windowMinutes) {
        this(maxAttempts, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    LoginRateLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Throws if this IP/email pair (or the email in general) has exhausted its failed attempts. */
    public void assertAllowed(String ip, String email) {
        if (count(pairKey(ip, email)) >= maxAttempts
                || count(emailKey(email)) >= maxAttempts * EMAIL_MULTIPLIER) {
            throw new TooManyLoginAttemptsException("Too many failed login attempts. Try again later.");
        }
    }

    public void recordFailure(String ip, String email) {
        if (failures.size() > CLEANUP_THRESHOLD) {
            failures.keySet().forEach(this::count);
        }
        add(pairKey(ip, email));
        add(emailKey(email));
    }

    /** A successful login clears the pair's counter (the email-wide counter keeps decaying on its own). */
    public void reset(String ip, String email) {
        failures.remove(pairKey(ip, email));
    }

    private void add(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            attempts.addLast(clock.instant());
        }
    }

    /** Number of failures inside the window; drops expired entries and empty keys. */
    private int count(String key) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) {
            return 0;
        }
        Instant cutoff = clock.instant().minus(window);
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.pollFirst();
            }
            if (attempts.isEmpty()) {
                failures.remove(key, attempts);
                return 0;
            }
            return attempts.size();
        }
    }

    private static String pairKey(String ip, String email) {
        return "pair|" + ip + "|" + normalize(email);
    }

    private static String emailKey(String email) {
        return "email|" + normalize(email);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
