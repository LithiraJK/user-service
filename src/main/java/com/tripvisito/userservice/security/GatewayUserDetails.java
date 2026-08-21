package com.tripvisito.userservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Immutable value object stored as the {@code details} field on the
 * {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken}
 * created by {@link GatewayAuthFilter}.
 *
 * <p>Controllers can access this via:
 * <pre>
 * var auth = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
 * var details = (GatewayUserDetails) auth.getDetails();
 * String userId = details.getUserId();
 * </pre>
 *
 * <p>Or via a utility method in the controller base class.
 */
@Getter
@AllArgsConstructor
public class GatewayUserDetails {
    private final String userId;
    private final String name;
    private final String email;
    private final String roles;
}
