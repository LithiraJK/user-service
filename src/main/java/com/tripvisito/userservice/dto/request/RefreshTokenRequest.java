package com.tripvisito.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for {@code POST /api/v1/auth/refresh} */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
