package com.tripvisito.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request body for {@code PUT /api/v1/auth/status/{id}} */
@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "isBlocked field is required")
    private Boolean isBlocked;
}
