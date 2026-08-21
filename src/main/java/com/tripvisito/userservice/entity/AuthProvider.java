package com.tripvisito.userservice.entity;

/**
 * Authentication provider enumeration.
 *
 * <p>Mirrors the original Node.js {@code AuthProvider} enum:
 * <pre>
 *   LOCAL   — User registered with email + password (BCrypt hashed).
 *   GOOGLE  — User authenticated via Google OAuth (no password stored).
 * </pre>
 *
 * Stored as a {@code VARCHAR} column on the {@code users} table.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
