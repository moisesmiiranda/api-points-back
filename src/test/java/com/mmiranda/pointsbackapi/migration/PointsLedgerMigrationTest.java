package com.mmiranda.pointsbackapi.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs V8 (points ledger, purchase bookkeeping, rewards) over legacy data written under the V7 schema:
 * existing purchases become CREDIT entries and any stored balance that does not match them becomes an
 * explicit ADJUST entry, so the ledger of every client adds up to its balance.
 */
class PointsLedgerMigrationTest {

    @Test
    void v8BuildsALedgerThatAddsUpToEveryExistingBalance() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:points_ledger_migration;DB_CLOSE_DELAY=-1", "sa", "");
        String[] locations = {"classpath:db/migration", "classpath:db/dev"};
        Flyway.configure().dataSource(dataSource).locations(locations).target("7").load().migrate();

        var jdbc = new JdbcTemplate(dataSource);
        // Establishment 1 (seed) has value_per_point 10. Client 1 (seed) has balance 0 but a 150.75 purchase
        // (15 points): a stored balance that disagrees with its purchases. Add a client whose balance matches
        // and one whose balance is higher than its purchases explain.
        jdbc.update("INSERT INTO person (cpf) VALUES ('52998224725')");
        jdbc.update("INSERT INTO person (cpf) VALUES ('11144477735')");
        long personA = jdbc.queryForObject("SELECT id FROM person WHERE cpf = '52998224725'", Long.class);
        long personB = jdbc.queryForObject("SELECT id FROM person WHERE cpf = '11144477735'", Long.class);
        jdbc.update("INSERT INTO client (id, name, person_id, points, establishment_id) VALUES (10, 'Matches', ?, 35, 1)", personA);
        jdbc.update("INSERT INTO client (id, name, person_id, points, establishment_id) VALUES (11, 'Bonus', ?, 50, 1)", personB);
        jdbc.update("INSERT INTO purchase (client_id, establishment_id, amount) VALUES (10, 1, 20.00)");   // 2 points
        jdbc.update("INSERT INTO purchase (client_id, establishment_id, amount) VALUES (10, 1, 339.99)");  // 33 points
        jdbc.update("INSERT INTO purchase (client_id, establishment_id, amount) VALUES (11, 1, 9.99)");    // 0 points
        int purchasesBefore = jdbc.queryForObject("SELECT COUNT(*) FROM purchase", Integer.class);

        Flyway.configure().dataSource(dataSource).locations(locations).load().migrate();

        assertEquals(purchasesBefore, (int) jdbc.queryForObject("SELECT COUNT(*) FROM purchase", Integer.class));
        // floor(amount / value_per_point) on the purchases
        assertEquals(15, jdbc.queryForObject("SELECT points_earned FROM purchase WHERE amount = 150.75", Integer.class));
        assertEquals(33, jdbc.queryForObject("SELECT points_earned FROM purchase WHERE amount = 339.99", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT points_earned FROM purchase WHERE amount = 9.99", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT points_redeemed FROM purchase WHERE amount = 9.99", Integer.class));

        // Every client's ledger adds up to its stored balance, and no balance changed
        for (long clientId : new long[] {1, 2, 10, 11}) {
            int balance = jdbc.queryForObject("SELECT points FROM client WHERE id = ?", Integer.class, clientId);
            int ledgerSum = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(points), 0) FROM points_ledger WHERE client_id = ?", Integer.class, clientId);
            assertEquals(balance, ledgerSum, "ledger of client " + clientId);
        }
        assertEquals(35, (int) jdbc.queryForObject("SELECT points FROM client WHERE id = 10", Integer.class));
        assertEquals(50, (int) jdbc.queryForObject("SELECT points FROM client WHERE id = 11", Integer.class));

        // Client 10: two CREDIT entries with a running balance and no reconciliation needed
        assertEquals(2, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_ledger WHERE client_id = 10 AND type = 'CREDIT'", Integer.class));
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM points_ledger WHERE client_id = 10 AND type = 'ADJUST'", Integer.class));
        assertEquals(35, (int) jdbc.queryForObject(
                "SELECT MAX(balance_after) FROM points_ledger WHERE client_id = 10", Integer.class));
        // Client 11: no points from purchases, so the 50 is an explicit reconciliation
        assertEquals(50, (int) jdbc.queryForObject(
                "SELECT points FROM points_ledger WHERE client_id = 11 AND type = 'ADJUST'", Integer.class));
        // Client 1: 15 points earned but a stored balance of 0 -> CREDIT +15 then ADJUST -15
        assertEquals(-15, (int) jdbc.queryForObject(
                "SELECT points FROM points_ledger WHERE client_id = 1 AND type = 'ADJUST'", Integer.class));

        // Defaults for the new establishment settings
        assertEquals("CATALOG", jdbc.queryForObject("SELECT reward_mode FROM establishment WHERE id = 1", String.class));
        // and the ledger rejects a zero change or a negative running balance
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO points_ledger (client_id, type, points, balance_after) VALUES (10, 'ADJUST', 0, 35)"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO points_ledger (client_id, type, points, balance_after) VALUES (10, 'ADJUST', -50, -15)"));
    }
}
