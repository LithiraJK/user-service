package com.tripvisito.userservice.entity;

/**
 * User role enumeration.
 *
 * <p>Maps directly from the original Node.js {@code Role} enum:
 * <pre>
 *   SUPERADMIN  — Platform owner; can manage admins and all users.
 *   ADMIN       — Staff; can manage regular users.
 *   USER        — Default role for self-registered users.
 * </pre>
 *
 * Stored as a {@code VARCHAR} in the {@code user_roles} join table
 * via {@code @ElementCollection + @Enumerated(EnumType.STRING)}.
 */
public enum Role {
    SUPERADMIN,
    ADMIN,
    USER
}
