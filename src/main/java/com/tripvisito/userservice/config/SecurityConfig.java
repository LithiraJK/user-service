package com.tripvisito.userservice.config;

import com.tripvisito.userservice.security.GatewayAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for user-service.
 *
 * <h3>Security strategy</h3>
 * <ul>
 *   <li>All URL-level auth is handled upstream by the api-gateway's
 *       {@code JwtAuthFilter}. Therefore, this config permits all requests at
 *       the HTTP security layer — the service trusts that the gateway already
 *       rejected unauthorized requests.</li>
 *   <li>{@link GatewayAuthFilter} runs before the standard auth filter to
 *       populate {@code SecurityContextHolder} from {@code X-User-*} headers.</li>
 *   <li>{@code @EnableMethodSecurity} activates {@code @PreAuthorize} annotations
 *       on service/controller methods, providing role-based access control (RBAC)
 *       evaluated against the gateway-populated {@code Authentication}.</li>
 *   <li>CSRF is disabled — the API is stateless (JWT-based, no cookies).</li>
 *   <li>Sessions are stateless — no {@code HttpSession} is created.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final GatewayAuthFilter gatewayAuthFilter;

    public SecurityConfig(GatewayAuthFilter gatewayAuthFilter) {
        this.gatewayAuthFilter = gatewayAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — not applicable for stateless REST APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless sessions — no HttpSession created or stored
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Permit all at the URL level — gateway enforces auth
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll())

            // Run GatewayAuthFilter before Spring's default auth filter
            .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder with cost factor 10 (same as the original
     * bcryptjs calls: {@code bcrypt.hash(password, 10)}).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
