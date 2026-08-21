package com.tripvisito.userservice.service;

import com.tripvisito.userservice.client.PaymentServiceClient;
import com.tripvisito.userservice.client.TripServiceClient;
import com.tripvisito.userservice.dto.response.UserResponse;
import com.tripvisito.userservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard analytics service.
 *
 * <p>Aggregates stats for the admin dashboard, migrating the logic from
 * {@code stats.controller.ts}. User-specific stats are sourced directly from
 * the MySQL database. Cross-service data (trip counts, latest payments) is
 * fetched via Feign clients to trip-service and payment-service, with
 * graceful fallback to empty data if those services are temporarily unavailable.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    private final UserRepository userRepository;
    private final TripServiceClient tripServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    public StatsService(UserRepository userRepository,
                        TripServiceClient tripServiceClient,
                        PaymentServiceClient paymentServiceClient) {
        this.userRepository = userRepository;
        this.tripServiceClient = tripServiceClient;
        this.paymentServiceClient = paymentServiceClient;
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    /**
     * Returns aggregated platform stats for the admin dashboard.
     * Mirrors: {@code GET /api/v1/dashboard/stats} → {@code getDashboardStats()}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public Map<String, Object> getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfLastMonth = startOfCurrentMonth.minusMonths(1);

        // ── User stats (from local MySQL) ─────────────────────────────────
        long totalUsers = userRepository.count();
        long usersThisMonth = userRepository.countByJoinedAtBetween(startOfCurrentMonth, now);
        long usersLastMonth = userRepository.countByJoinedAtBetween(startOfLastMonth, startOfCurrentMonth);
        long activeThisMonth = userRepository.countByLastLoginBetween(startOfCurrentMonth, now);
        long activeLastMonth = userRepository.countByLastLoginBetween(startOfLastMonth, startOfCurrentMonth);

        List<Integer> userTrend = extractCounts(userRepository.findUserJoinTrend(sevenDaysAgo));
        List<Integer> activeTrend = extractCounts(userRepository.findActiveUserTrend(sevenDaysAgo));

        // ── Trip stats (from trip-service via Feign) ─────────────────────
        Map<String, Object> tripStats = fetchTripStats();

        // ── Payment stats (from payment-service via Feign) ───────────────
        Map<String, Object> paymentStats = fetchPaymentStats();

        // ── Assemble response ─────────────────────────────────────────────
        Map<String, Object> result = new HashMap<>();
        result.put("users", Map.of(
                "total", totalUsers,
                "currentMonth", usersThisMonth,
                "lastMonth", usersLastMonth,
                "trend", userTrend
        ));
        result.put("trips", tripStats);
        result.put("active", Map.of(
                "total", activeThisMonth,
                "currentMonth", activeThisMonth,
                "lastMonth", activeLastMonth,
                "trend", activeTrend
        ));
        result.put("payments", paymentStats);

        return result;
    }

    // ── User Growth Chart ─────────────────────────────────────────────────────

    /**
     * Returns all-time daily user registration counts for the growth chart.
     * Mirrors: {@code GET /api/v1/dashboard/user-growth} → {@code getUserGrowth()}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public List<Map<String, Object>> getUserGrowth() {
        return userRepository.findUserGrowthByDate().stream()
                .map(row -> Map.<String, Object>of(
                        "date", row[0].toString(),
                        "count", ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // ── Latest Signups ────────────────────────────────────────────────────────

    /**
     * Returns the 5 most recently registered users for the admin dashboard widget.
     * Mirrors: {@code GET /api/v1/auth/latest-users} → {@code getLatestUserSignups()}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public List<UserResponse> getLatestUserSignups() {
        return userRepository.findTop5ByOrderByJoinedAtDesc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    // ── Cross-Service Calls (with graceful fallback) ──────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchTripStats() {
        try {
            return (Map<String, Object>) tripServiceClient.getInternalStats().get("data");
        } catch (Exception e) {
            log.warn("[StatsService] trip-service unavailable, using empty stats: {}", e.getMessage());
            return Map.of("total", 0, "currentMonth", 0, "lastMonth", 0, "trend", List.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPaymentStats() {
        try {
            return (Map<String, Object>) paymentServiceClient.getInternalStats().get("data");
        } catch (Exception e) {
            log.warn("[StatsService] payment-service unavailable, using empty stats: {}", e.getMessage());
            return Map.of("latest", List.of());
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Extracts the count (second column) from JPQL {@code Object[]} result rows. */
    private List<Integer> extractCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> ((Number) row[1]).intValue())
                .toList();
    }
}
