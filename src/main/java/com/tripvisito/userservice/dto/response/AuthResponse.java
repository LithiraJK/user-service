package com.tripvisito.userservice.dto.response;

import com.tripvisito.userservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO returned on successful authentication (login / register).
 *
 * <p>Mirrors the original Express {@code sendSuccess} payload for auth:
 * <pre>
 * {
 *   "accessToken": "eyJ...",
 *   "refreshToken": "eyJ...",
 *   "user": { "id": ..., "email": ..., "name": ..., "roles": [...] }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserSummary user;

    /**
     * Minimal user information included with every auth response.
     * Exposes only the fields needed by the React frontend — no password hash.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String email;
        private String name;
        private Set<Role> roles;
        private String profileImg;

        /**
         * Lowercase alias getter for backward compatibility with the React frontend.
         */
        @com.fasterxml.jackson.annotation.JsonProperty("profileimg")
        public String getProfileimg() {
            return profileImg;
        }
    }
}
