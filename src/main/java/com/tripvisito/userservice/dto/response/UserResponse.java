package com.tripvisito.userservice.dto.response;

import com.tripvisito.userservice.entity.AuthProvider;
import com.tripvisito.userservice.entity.Role;
import com.tripvisito.userservice.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Full user profile response DTO.
 * Returned by {@code GET /api/v1/auth/me} and user listing endpoints.
 * Excludes {@code passwordHash} — never exposed to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private Set<Role> roles;
    private boolean blocked;
    private AuthProvider authProvider;
    private String profileImg;
    private LocalDateTime joinedAt;
    private LocalDateTime lastLogin;

    /**
     * Static factory — converts a {@link User} entity to a safe response DTO.
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles())
                .blocked(user.isBlocked())
                .authProvider(user.getAuthProvider())
                .profileImg(user.getProfileImg())
                .joinedAt(user.getJoinedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    /**
     * Lowercase alias getter for backward compatibility with the React frontend.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("profileimg")
    public String getProfileimg() {
        return profileImg;
    }
}
