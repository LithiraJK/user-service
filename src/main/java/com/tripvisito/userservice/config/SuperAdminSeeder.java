package com.tripvisito.userservice.config;

import com.tripvisito.userservice.entity.AuthProvider;
import com.tripvisito.userservice.entity.Role;
import com.tripvisito.userservice.entity.User;
import com.tripvisito.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Application startup component that seeds a SUPERADMIN user if none exists.
 *
 * <p>Mirrors the {@code createSuperAdmin()} function from the original
 * {@code auth.controller.ts}, which was called once on MongoDB connection.
 *
 * <p>Runs after the application context is fully initialized (including JPA/Hibernate
 * schema creation) via {@link ApplicationRunner}, which guarantees that the
 * {@code users} and {@code user_roles} tables exist before the insert attempt.
 *
 * <p>Configuration is sourced from config-server ({@code configs/user-service.yml}):
 * <pre>
 *   superadmin.email    = ${SUPERADMIN_EMAIL}
 *   superadmin.password = ${SUPERADMIN_PASSWORD}
 *   superadmin.name     = ${SUPERADMIN_NAME}
 * </pre>
 */
@Component
public class SuperAdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${superadmin.email}")
    private String superAdminEmail;

    @Value("${superadmin.password}")
    private String superAdminPassword;

    @Value("${superadmin.name}")
    private String superAdminName;

    public SuperAdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("[SuperAdminSeeder] Users already exist — skipping seed.");
            return;
        }

        // 1. Super Admin (ID 1)
        User superAdmin = User.builder()
                .email(superAdminEmail)
                .name(superAdminName)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .roles(Set.of(Role.SUPERADMIN))
                .blocked(false)
                .authProvider(AuthProvider.LOCAL)
                .joinedAt(LocalDateTime.now().minusDays(10))
                .lastLogin(LocalDateTime.now())
                .profileImg("https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?auto=format&fit=facearea&facepad=2&w=256&h=256&q=80")
                .build();

        // 2. Admin (ID 2)
        User admin = User.builder()
                .email("admin@tripvisito.com")
                .name("Admin User")
                .passwordHash(passwordEncoder.encode("changeme123"))
                .roles(Set.of(Role.ADMIN))
                .blocked(false)
                .authProvider(AuthProvider.LOCAL)
                .joinedAt(LocalDateTime.now().minusDays(8))
                .lastLogin(LocalDateTime.now())
                .profileImg("https://images.unsplash.com/photo-1519345182560-3f2917c472ef?auto=format&fit=facearea&facepad=2&w=256&h=256&q=80")
                .build();

        // 3. Regular User (ID 3)
        User regularUser = User.builder()
                .email("john@tripvisito.com")
                .name("John Doe")
                .passwordHash(passwordEncoder.encode("changeme123"))
                .roles(Set.of(Role.USER))
                .blocked(false)
                .authProvider(AuthProvider.LOCAL)
                .joinedAt(LocalDateTime.now().minusDays(5))
                .lastLogin(LocalDateTime.now())
                .profileImg("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=facearea&facepad=2&w=256&h=256&q=80")
                .build();

        userRepository.saveAll(List.of(superAdmin, admin, regularUser));
        log.info("[SuperAdminSeeder] 3 users seeded successfully.");
    }
}
