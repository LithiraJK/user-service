package com.tripvisito.userservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security filter that reads the {@code X-User-*} headers injected by
 * the API Gateway's {@code JwtAuthFilter} and populates the
 * {@link SecurityContextHolder} for the current request thread.
 *
 * <h3>Why this filter exists</h3>
 * <p>JWT validation is centralized at the api-gateway — user-service never
 * sees raw tokens on protected routes. However, Spring Security's
 * {@code @PreAuthorize} and {@code @Secured} annotations require an
 * {@code Authentication} object in the SecurityContext to evaluate role
 * expressions. This filter bridges the gap by converting gateway-injected
 * headers into a Spring {@code Authentication} object.
 *
 * <h3>Headers consumed</h3>
 * <table>
 *   <tr><th>Header</th><th>Example value</th><th>Maps to</th></tr>
 *   <tr><td>{@code X-User-Id}</td><td>{@code 42}</td><td>principal name</td></tr>
 *   <tr><td>{@code X-User-Name}</td><td>{@code Alice}</td><td>stored in details</td></tr>
 *   <tr><td>{@code X-User-Email}</td><td>{@code alice@x.com}</td><td>stored in details</td></tr>
 *   <tr><td>{@code X-User-Roles}</td><td>{@code [USER]}</td><td>GrantedAuthority list</td></tr>
 * </table>
 */
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);

    private static final String HEADER_USER_ID    = "X-User-Id";
    private static final String HEADER_USER_NAME  = "X-User-Name";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(HEADER_USER_ID);

        // If gateway did not inject X-User-Id, this is a public/unauthenticated request.
        // Let Spring Security handle it as an anonymous request.
        if (userId == null || userId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String rolesHeader = request.getHeader(HEADER_USER_ROLES);
            List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);

            // Build Spring Security's Authentication object
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Store extra user details for use in controllers
            authentication.setDetails(new GatewayUserDetails(
                    userId,
                    request.getHeader(HEADER_USER_NAME),
                    request.getHeader(HEADER_USER_EMAIL),
                    rolesHeader
            ));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[GatewayAuthFilter] Authenticated userId={} roles={}", userId, rolesHeader);

        } catch (Exception e) {
            log.warn("[GatewayAuthFilter] Failed to parse user headers: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parses the {@code X-User-Roles} header value into Spring Security authorities.
     *
     * <p>The gateway forwards the roles claim from the JWT's {@code roles} array
     * via {@code claims.get("roles").toString()}, which produces strings like:
     * {@code [USER]}, {@code [SUPERADMIN]}, or {@code [ADMIN, USER]}.
     *
     * <p>Spring Security's {@code hasRole()} checks expect the prefix {@code ROLE_},
     * so each role name is prefixed accordingly.
     */
    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Collections.emptyList();
        }

        // Strip surrounding brackets if present: "[USER]" → "USER"
        String cleaned = rolesHeader.replaceAll("[\\[\\]\\s]", "");

        return Arrays.stream(cleaned.split(","))
                .filter(r -> !r.isBlank())
                .map(role -> {
                    String cleanRole = role.trim().toUpperCase();
                    if (!cleanRole.startsWith("ROLE_")) {
                        cleanRole = "ROLE_" + cleanRole;
                    }
                    return new SimpleGrantedAuthority(cleanRole);
                })
                .collect(Collectors.toList());
    }
}
