package com.tripvisito.userservice.controller;

import com.tripvisito.userservice.dto.response.ApiResponse;
import com.tripvisito.userservice.dto.response.UserResponse;
import com.tripvisito.userservice.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for admin dashboard analytics.
 *
 * <p>Migrates the {@code stats.routes.ts} endpoints ({@code /api/v1/dashboard/**})
 * into user-service, which owns the user analytics data (MySQL) and
 * orchestrates cross-service calls for trip/payment stats.
 *
 * <p>All endpoints require ADMIN or SUPERADMIN role — enforced via
 * {@code @PreAuthorize} inside {@link StatsService}.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final StatsService statsService;

    public DashboardController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * GET /api/v1/dashboard/stats
     * Returns users, trips, active users, and payments summary.
     * Mirrors: {@code getDashboardStats()}
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = statsService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats fetched successfully", stats));
    }

    /**
     * GET /api/v1/dashboard/user-growth
     * Returns all-time daily user registration counts for the growth chart.
     * Mirrors: {@code getUserGrowth()}
     */
    @GetMapping("/user-growth")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserGrowth() {
        List<Map<String, Object>> growth = statsService.getUserGrowth();
        return ResponseEntity.ok(ApiResponse.success("User growth data fetched successfully", growth));
    }

    /**
     * GET /api/v1/dashboard/latest-users
     * Returns the 5 most recently registered users.
     * Mirrors: {@code getLatestUserSignups()} (originally on auth.routes.ts)
     */
    @GetMapping("/latest-users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getLatestUserSignups() {
        List<UserResponse> users = statsService.getLatestUserSignups();
        return ResponseEntity.ok(ApiResponse.success("Latest users fetched successfully", users));
    }
}
