package com.tripvisito.userservice.util;

import com.tripvisito.userservice.entity.Role;
import com.tripvisito.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT token factory and validator for user-service.
 *
 * <p>This service is the <b>only issuer</b> of Tripvisito JWTs. The api-gateway
 * only validates them via its own {@code JwtUtil}. Both share the same secret
 * ({@code jwt.secret}) sourced from config-server.
 *
 * <h3>Token Structure</h3>
 * <b>Access Token</b> claims:
 * <ul>
 *   <li>{@code sub}   — User ID (Long as String)</li>
 *   <li>{@code name}  — User's display name</li>
 *   <li>{@code email} — User's email address</li>
 *   <li>{@code roles} — List of role names (e.g. {@code ["USER"]})</li>
 * </ul>
 *
 * <b>Refresh Token</b> claims:
 * <ul>
 *   <li>{@code sub} — User ID only (minimal claims for security)</li>
 * </ul>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;             // default: 86_400_000 (1 day)

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;      // default: 604_800_000 (7 days)

    // ── Key Construction ─────────────────────────────────────────────────────

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Token Issuance ───────────────────────────────────────────────────────

    /**
     * Issues a signed JWT access token for the given user.
     * Claims include: sub (userId), name, email, roles.
     */
    public String generateAccessToken(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("profileImg", user.getProfileImg())
                .claim("roles", roleNames)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Issues a refresh token containing only the user's ID.
     * Minimal claims reduce the risk of information leakage if the refresh
     * token is compromised.
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token Validation & Extraction ────────────────────────────────────────

    /**
     * Parses and validates a token, returning all embedded claims.
     *
     * @throws io.jsonwebtoken.JwtException on invalid/expired tokens
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extracts the subject (userId) from a validated token. */
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Returns {@code true} if the token is valid and not expired. */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
