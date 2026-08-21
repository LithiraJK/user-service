package com.tripvisito.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a platform user.
 *
 * <p>Migrated from the MongoDB {@code user.model.ts} (Mongoose) schema to a
 * relational MySQL table. Key design decisions:
 *
 * <ul>
 *   <li><b>Roles</b> are stored in a separate {@code user_roles} join table
 *       via {@code @ElementCollection}, preserving the original multi-role design
 *       (a user can have {@code [SUPERADMIN]} or {@code [ADMIN]} or {@code [USER]}).</li>
 *   <li><b>Password</b> is nullable because Google OAuth users have no local password.</li>
 *   <li><b>profileImg</b> stores a URL string — image upload is handled externally
 *       (originally Cloudinary; in the Spring migration, admins pass a URL directly,
 *       or the frontend pre-uploads to GCP Storage via trip-service).</li>
 *   <li>{@code joinedAt} and {@code lastLogin} mirror the Mongoose schema fields and
 *       are used for dashboard analytics in {@code StatsService}.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Primary login identifier. Must be unique across the platform.
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 150)
    private String name;

    /**
     * BCrypt-hashed password. Null for Google OAuth users.
     * Never exposed in response DTOs.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * User roles stored in {@code user_roles(user_id, role)} join table.
     * Fetched eagerly so SecurityContext population in GatewayAuthFilter
     * does not trigger lazy-load exceptions outside a transaction.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * When true, the user is prevented from logging in.
     * Admins can toggle this via {@code PUT /api/v1/auth/status/{id}}.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    /**
     * Tracks how the user authenticated. Determines whether {@code passwordHash}
     * is required on login.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /**
     * Public URL of the user's profile image.
     * Defaults to a generic avatar if not set.
     */
    @Column(name = "profile_img", length = 512)
    @Builder.Default
    private String profileImg = "https://img.freepik.com/premium-vector/vector-flat-illustration-grayscale-avatar-user-profile-person-icon-profile-picture-business-profile-woman-suitable-social-media-profiles-icons-screensavers-as-templatex9_719432-1339.jpg";

    /**
     * Timestamp of first account creation. Used for user growth analytics.
     * Set once on creation, never updated.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    /**
     * Updated on every successful login. Used for active-user analytics.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime lastLogin = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
