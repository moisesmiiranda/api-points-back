package com.mmiranda.pointsbackapi.security;

import com.mmiranda.pointsbackapi.model.Role;
import com.mmiranda.pointsbackapi.model.User;
import com.mmiranda.pointsbackapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures at least one PLATFORM_ADMIN account exists so the system is bootstrappable
 * after a fresh deployment, when no account can yet create another account.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrapRunner(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${admin.bootstrap.email}") String adminEmail,
                                 @Value("${admin.bootstrap.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .name("Platform Admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.PLATFORM_ADMIN)
                .establishment(null)
                .active(true)
                .build();

        userRepository.save(admin);
    }
}
