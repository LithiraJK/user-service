package com.tripvisito.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified API response envelope.
 *
 * <p>Mirrors the shape of the original Express {@code sendSuccess} / {@code sendError}
 * utilities from {@code api.response.util.ts}:
 * <pre>
 * {
 *   "status": 200,
 *   "message": "Trip retrieved successfully",
 *   "data": { ... }
 * }
 * </pre>
 *
 * {@code @JsonInclude(NON_NULL)} ensures {@code data} is omitted from
 * error responses that have no payload.
 *
 * @param <T> the type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    // ── Factory methods ──────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(200, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .status(200)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
}
