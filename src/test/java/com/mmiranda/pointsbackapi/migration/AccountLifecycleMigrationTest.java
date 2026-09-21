package com.mmiranda.pointsbackapi.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** V9 must not lock anybody out: existing establishments stay ACTIVE and existing users do not have to change their password. */
class AccountLifecycleMigrationTest {

    @Test
    void v9KeepsExistingEstablishmentsActiveAndExistingUsersFree() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:account_lifecycle_migration;DB_CLOSE_DELAY=-1", "sa", "");
        String[] locations = {"classpath:db/migration", "classpath:db/dev"};
        Flyway.configure().dataSource(dataSource).locations(locations).target("8").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO establishment (name, value_per_point, cnpj) VALUES ('Legacy Shop', 10, '33.333.333/0001-33')");
        int establishments = jdbc.queryForObject("SELECT COUNT(*) FROM establishment", Integer.class);
        int users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

        Flyway.configure().dataSource(dataSource).locations(locations).load().migrate();

        assertEquals(establishments, (int) jdbc.queryForObject("SELECT COUNT(*) FROM establishment", Integer.class));
        assertEquals(establishments, (int) jdbc.queryForObject("SELECT COUNT(*) FROM establishment WHERE status = 'ACTIVE'", Integer.class));
        assertEquals(0, (int) jdbc.queryForObject("SELECT COUNT(*) FROM establishment WHERE trial_ends_at IS NOT NULL", Integer.class));
        assertEquals(users, (int) jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE must_change_password = FALSE", Integer.class));
        assertEquals(users, (int) jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE password_changed_at IS NULL", Integer.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("UPDATE establishment SET status = 'BROKEN' WHERE name = 'Legacy Shop'"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO password_reset_token (user_id, token_hash, type, expires_at) VALUES (1, 'h', 'OTHER', CURRENT_TIMESTAMP)"));
    }
}
