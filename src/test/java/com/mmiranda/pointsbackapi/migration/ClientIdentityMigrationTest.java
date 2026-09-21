package com.mmiranda.pointsbackapi.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Runs V7 (person + client identity) over legacy data written under the V6 schema and checks that
 * nothing is lost: same client ids, balances, contact data and purchase links.
 */
class ClientIdentityMigrationTest {

    @Test
    void v7KeepsClientsBalancesAndPurchasesAndLinksTheSameCpfToOnePerson() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:client_identity_migration;DB_CLOSE_DELAY=-1", "sa", "");
        String[] locations = {"classpath:db/migration", "classpath:db/dev"};
        Flyway.configure().dataSource(dataSource).locations(locations).target("6").load().migrate();

        var jdbc = new JdbcTemplate(dataSource);
        // Legacy rows: the same person under two CPF spellings in two establishments (allowed by the old unique(cpf))
        jdbc.update("INSERT INTO client (id, name, email, phone, cpf, points, establishment_id) VALUES (3, 'Ana at 1', 'ana1@x.com', '11999990000', '529.982.247-25', 40, 1)");
        jdbc.update("INSERT INTO client (id, name, email, phone, cpf, points, establishment_id) VALUES (4, 'Ana at 2', 'ana2@x.com', '21888880000', '52998224725', 25, 2)");
        jdbc.update("INSERT INTO purchase (client_id, establishment_id, amount) VALUES (3, 1, 20.00)");
        int clientsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM client", Integer.class);
        int purchasesBefore = jdbc.queryForObject("SELECT COUNT(*) FROM purchase", Integer.class);

        Flyway.configure().dataSource(dataSource).locations(locations).load().migrate();

        assertEquals(clientsBefore, (int) jdbc.queryForObject("SELECT COUNT(*) FROM client", Integer.class));
        assertEquals(purchasesBefore, (int) jdbc.queryForObject("SELECT COUNT(*) FROM purchase", Integer.class));
        // 2 seeded CPFs + 1 person shared by the two legacy spellings
        assertEquals(3, (int) jdbc.queryForObject("SELECT COUNT(*) FROM person", Integer.class));

        long personOfThird = jdbc.queryForObject("SELECT person_id FROM client WHERE id = 3", Long.class);
        long personOfFourth = jdbc.queryForObject("SELECT person_id FROM client WHERE id = 4", Long.class);
        assertEquals(personOfThird, personOfFourth);
        assertEquals("52998224725", jdbc.queryForObject("SELECT cpf FROM person WHERE id = ?", String.class, personOfThird));
        assertNotEquals(1L, (long) jdbc.queryForObject("SELECT establishment_id FROM client WHERE id = 4", Long.class));

        assertEquals(40, (int) jdbc.queryForObject("SELECT points FROM client WHERE id = 3", Integer.class));
        assertEquals(25, (int) jdbc.queryForObject("SELECT points FROM client WHERE id = 4", Integer.class));
        assertEquals("ana2@x.com", jdbc.queryForObject("SELECT email FROM client WHERE id = 4", String.class));
        assertEquals(3, (int) jdbc.queryForObject("SELECT client_id FROM purchase WHERE amount = 20.00", Integer.class));

        // Seeded clients still point at their (normalized) CPF
        assertEquals("12345678900", jdbc.queryForObject(
                "SELECT p.cpf FROM client c JOIN person p ON p.id = c.person_id WHERE c.id = 1", String.class));
        // The CPF now lives only on person
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE UPPER(table_name) = 'CLIENT' AND UPPER(column_name) = 'CPF'",
                Integer.class));
        // and one account per (person, establishment) is enforced
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO client (name, person_id, establishment_id, points) VALUES ('Dup', ?, 1, 0)", personOfThird));
    }
}
