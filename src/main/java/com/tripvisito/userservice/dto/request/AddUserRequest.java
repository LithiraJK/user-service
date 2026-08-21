package com.tripvisito.userservice.dto.request;

import com.tripvisito.userservice.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * Request body for {@code POST /api/v1/auth/register/new-user}.
 * Used by ADMIN / SUPERADMIN to manually create a user account
 * with a specific role assignment.
 *
 * <p>In the original Express code, a profile image file could be uploaded
 * via multipart/form-data. In the Spring migration, pass a {@code profileImgUrl}
 * string instead (the frontend or admin pre-uploads images separately).
 */
@Data
public class AddUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150)
    private String name;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotEmpty(message = "At least one role must be specified")
    private Set<Role> roles;

    /** Optional URL of a pre-uploaded profile image. */
    private String profileImgUrl;
}
