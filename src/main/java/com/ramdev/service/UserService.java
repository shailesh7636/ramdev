package com.ramdev.service;

import com.ramdev.dto.CreateUserRequest;
import com.ramdev.entity.Role;
import com.ramdev.entity.User;
import com.ramdev.repository.RoleRepository;
import com.ramdev.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "12345";

    // ── Queries ──────────────────────────────────────────────────

    public List<User> getUsersByRole(String roleName) {
        return userRepository.findAllByRoles_Name(roleName);
    }

    /** Returns only users with the USER role (for user management views). */
    public List<User> getAllUsers() {
        return userRepository.findAllByRoles_Name("USER");
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    // ── Mutations ────────────────────────────────────────────────

    /**
     * Creates a user with the given role and a BCrypt-hashed default password.
     * SUPER_ADMIN cannot be assigned through this method (only DataSeeder does that).
     */
    @Transactional
    public User createUser(CreateUserRequest req) {
        // Input validation
        if (req.getMobile() == null || !req.getMobile().matches("\\d{10}")) {
            throw new IllegalArgumentException("Mobile number must be exactly 10 digits.");
        }
        if (userRepository.existsByMobile(req.getMobile())) {
            throw new IllegalArgumentException("Mobile number already registered.");
        }

        String roleName = req.getRole().toUpperCase().trim();
        if ("SUPER_ADMIN".equals(roleName)) {
            throw new IllegalArgumentException("SUPER_ADMIN cannot be created via this form.");
        }

        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleName));

        User user = new User();
        user.setName(req.getName().trim());
        user.setMobile(req.getMobile().trim());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));   // BCrypt
        user.setRoles(new HashSet<>(Set.of(role)));

        return userRepository.save(user);
    }

    /** Deletes a user. Super Admin can never be deleted. */
    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        if (user.hasRole("SUPER_ADMIN")) {
            throw new IllegalStateException("The Super Admin account cannot be deleted.");
        }
        userRepository.delete(user);
    }
}
