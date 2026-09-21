package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimiterTest {

    private static final String IP = "203.0.113.7";
    private static final String EMAIL = "owner@test.com";

    private MutableClock clock;
    private LoginRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
        limiter = new LoginRateLimiter(3, Duration.ofMinutes(15), clock);
    }

    @Test
    void allowsAttemptsBelowTheLimit() {
        limiter.recordFailure(IP, EMAIL);
        limiter.recordFailure(IP, EMAIL);

        assertDoesNotThrow(() -> limiter.assertAllowed(IP, EMAIL));
    }

    @Test
    void blocksThePairOnceTheLimitIsReached() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(IP, EMAIL);
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.assertAllowed(IP, EMAIL));
    }

    @Test
    void emailMatchingIsCaseAndWhitespaceInsensitive() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(IP, "  Owner@Test.com ");
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.assertAllowed(IP, EMAIL));
    }

    @Test
    void otherIpsAreNotBlockedByAPairButTheEmailAloneEventuallyIs() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(IP, EMAIL);
        }
        assertDoesNotThrow(() -> limiter.assertAllowed("198.51.100.9", EMAIL));

        // Distributed guessing: failures from many IPs against the same email
        for (int i = 0; i < 3 * LoginRateLimiter.EMAIL_MULTIPLIER; i++) {
            limiter.recordFailure("10.0.0." + i, EMAIL);
        }
        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.assertAllowed("198.51.100.9", EMAIL));
    }

    @Test
    void failuresExpireAfterTheWindow() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(IP, EMAIL);
        }
        clock.advance(Duration.ofMinutes(16));

        assertDoesNotThrow(() -> limiter.assertAllowed(IP, EMAIL));
    }

    @Test
    void successfulLoginResetsThePairCounter() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(IP, EMAIL);
        }
        limiter.reset(IP, EMAIL);

        assertDoesNotThrow(() -> limiter.assertAllowed(IP, EMAIL));
    }

    @Test
    void nullEmailIsTreatedAsEmpty() {
        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(IP, null);
        }

        assertThrows(TooManyLoginAttemptsException.class, () -> limiter.assertAllowed(IP, null));
    }

    @Test
    void publicConstructorBuildsAWorkingLimiter() {
        LoginRateLimiter real = new LoginRateLimiter(1, 1);
        real.recordFailure(IP, EMAIL);

        assertThrows(TooManyLoginAttemptsException.class, () -> real.assertAllowed(IP, EMAIL));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
