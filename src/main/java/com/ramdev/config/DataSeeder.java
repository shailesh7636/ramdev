package com.ramdev.config;

import com.ramdev.entity.Role;
import com.ramdev.entity.User;
import com.ramdev.repository.RoleRepository;
import com.ramdev.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Runs on every startup.
 * 1. Creates the three roles if they don't exist.
 * 2. Creates / updates the Super Admin user.
 *
 * Using JPA here (not raw SQL) avoids the INSERT IGNORE / duplicate-key
 * problem that occurred when data.sql and this seeder both tried to
 * insert the same rows.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository  roleRepository;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "12345";

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedDefaultUser("6952939447", "Mukeshbhai (Owner)", "SUPER_ADMIN");
        seedDefaultUser("9624744024", "Admin User",         "ADMIN");
        seedDefaultUser("9624744027", "Default User",       "USER");
    }

    private void seedRoles() {
        seedRole("SUPER_ADMIN");
        seedRole("ADMIN");
        seedRole("USER");
        log.info("[DataSeeder] Roles ready.");
    }

    private void seedRole(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(new Role(name));
            log.info("[DataSeeder] Created role: {}", name);
        }
    }

    private void seedDefaultUser(String mobile, String name, String roleName) {
        // Skip entirely if user already exists — avoids BCrypt encode on every restart
        if (userRepository.existsByMobile(mobile)) {
            log.debug("[DataSeeder] {} already exists, skipping → mobile: {}", roleName, mobile);
            return;
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role missing after seed"));

        User user = new User();
        user.setName(name);
        user.setMobile(mobile);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRoles(new HashSet<>(Set.of(role)));

        userRepository.save(user);
        log.info("[DataSeeder] {} created → mobile: {}", roleName, mobile);
    }
}
