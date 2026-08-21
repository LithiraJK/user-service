package com.tripvisito.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Tripvisito User Service
 *
 * <p>Owns all user identity and authentication concerns for the Tripvisito platform:
 * <ul>
 *   <li><b>Authentication:</b> Local (email+password via BCrypt), Google OAuth,
 *       JWT access + refresh token issuance.</li>
 *   <li><b>User Management:</b> CRUD operations on users, role assignment
 *       (SUPERADMIN / ADMIN / USER), block/unblock status.</li>
 *   <li><b>Dashboard Stats:</b> Admin analytics — user growth, active users,
 *       latest signups. Cross-service data (trip counts, latest payments) is
 *       fetched via Feign clients from trip-service and payment-service.</li>
 * </ul>
 *
 * <p><b>Database:</b> MySQL — {@code tripvisito_users} schema (JPA / Hibernate).
 *
 * <p><b>Auth strategy:</b> The api-gateway validates JWTs and forwards user
 * identity via {@code X-User-*} headers. This service reads those headers in
 * {@link com.tripvisito.userservice.security.GatewayAuthFilter} to populate
 * Spring Security's {@code SecurityContextHolder}, enabling {@code @PreAuthorize}
 * method-security annotations throughout the service.
 *
 * <p><b>Port:</b> 8081 (configured via config-server)
 *
 * @author Tripvisito ECA Team
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
