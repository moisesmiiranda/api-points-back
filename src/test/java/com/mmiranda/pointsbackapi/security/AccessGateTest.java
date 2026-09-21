package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.model.EstablishmentStatus;
import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 20);

    private User user(Role role, EstablishmentStatus status, boolean mustChange) {
        Establishment establishment = null;
        if (role != Role.PLATFORM_ADMIN) {
            establishment = new Establishment();
            establishment.setId(7L);
            establishment.setStatus(status);
        }
        return User.builder().id(1L).role(role).establishment(establishment).mustChangePassword(mustChange).build();
    }

    @Test
    void anActiveAccountIsNeverBlocked() {
        assertTrue(AccessGate.check(user(Role.ESTABLISHMENT_STAFF, EstablishmentStatus.ACTIVE, false), "POST", "/purchases", TODAY).isEmpty());
        assertTrue(AccessGate.check(user(Role.PLATFORM_ADMIN, null, false), "DELETE", "/users/9", TODAY).isEmpty());
    }

    @Test
    void aSuspendedEstablishmentGets402ForEverythingExceptItsOwnProfileAndPlan() {
        User owner = user(Role.ESTABLISHMENT_OWNER, EstablishmentStatus.SUSPENDED, false);

        Optional<AccessGate.Block> blocked = AccessGate.check(owner, "GET", "/clients/all", TODAY);
        assertEquals(HttpStatus.PAYMENT_REQUIRED, blocked.orElseThrow().status());
        assertEquals(AccessGate.SUSPENDED_MESSAGE, blocked.get().message());
        assertEquals(HttpStatus.PAYMENT_REQUIRED, AccessGate.check(owner, "POST", "/purchases", TODAY).orElseThrow().status());
        assertEquals(HttpStatus.PAYMENT_REQUIRED, AccessGate.check(owner, "POST", "/users/me/password", TODAY).orElseThrow().status());
        // reading the plan of ANOTHER establishment is not on the allow list
        assertEquals(HttpStatus.PAYMENT_REQUIRED, AccessGate.check(owner, "GET", "/establishments/8/plan", TODAY).orElseThrow().status());

        assertTrue(AccessGate.check(owner, "GET", "/users/me", TODAY).isEmpty());
        assertTrue(AccessGate.check(owner, "GET", "/establishments/7/plan", TODAY).isEmpty());
    }

    @Test
    void anEndedTrialIsBlockedLikeASuspension() {
        Establishment establishment = new Establishment();
        establishment.setId(7L);
        establishment.setStatus(EstablishmentStatus.TRIAL);
        establishment.setTrialEndsAt(TODAY.minusDays(1));
        User user = User.builder().id(1L).role(Role.ESTABLISHMENT_STAFF).establishment(establishment).build();

        assertEquals(HttpStatus.PAYMENT_REQUIRED, AccessGate.check(user, "GET", "/clients/all", TODAY).orElseThrow().status());
    }

    @Test
    void aTemporaryPasswordGets403ForEverythingExceptReadingTheProfileAndChangingIt() {
        User staff = user(Role.ESTABLISHMENT_STAFF, EstablishmentStatus.ACTIVE, true);

        AccessGate.Block blocked = AccessGate.check(staff, "GET", "/clients/all", TODAY).orElseThrow();
        assertEquals(HttpStatus.FORBIDDEN, blocked.status());
        assertEquals(AccessGate.PASSWORD_CHANGE_MESSAGE, blocked.message());
        assertEquals(HttpStatus.FORBIDDEN, AccessGate.check(staff, "PUT", "/users/me", TODAY).orElseThrow().status());
        assertEquals(HttpStatus.FORBIDDEN, AccessGate.check(staff, "GET", "/users/me/password", TODAY).orElseThrow().status());

        assertTrue(AccessGate.check(staff, "GET", "/users/me", TODAY).isEmpty());
        assertTrue(AccessGate.check(staff, "POST", "/users/me/password", TODAY).isEmpty());
    }

    @Test
    void suspensionWinsWhenBothApply() {
        User both = user(Role.ESTABLISHMENT_OWNER, EstablishmentStatus.SUSPENDED, true);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, AccessGate.check(both, "GET", "/clients/all", TODAY).orElseThrow().status());
    }
}
