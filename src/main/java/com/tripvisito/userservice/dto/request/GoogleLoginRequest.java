package com.tripvisito.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code POST /api/v1/auth/google-login}.
 * Sent by the React frontend after completing Google OAuth client-side
 * (using Firebase Auth or Google Identity Services SDK).
 */
@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Name is required")
    private String name;

    /** Google profile picture URL. Optional. */
    @com.fasterxml.jackson.annotation.JsonAlias("profileimg")
    private String profileImg;
}
