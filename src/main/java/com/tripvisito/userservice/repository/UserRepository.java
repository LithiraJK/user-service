package com.tripvisito.userservice.repository;

import com.tripvisito.userservice.entity.Role;
import com.tripvisito.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository},
 * plus custom JPQL queries used by {@link com.tripvisito.userservice.service.StatsService}
 * for the admin dashboard analytics.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Auth Queries ─────────────────────────────────────────────────────────

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // ── Role Queries ─────────────────────────────────────────────────────────

    /**
     * Checks if a super admin already exists. Used by {@link com.tripvisito.userservice.config.SuperAdminSeeder}
     * to avoid duplicating the seed account on every restart.
     */
    boolean existsByRolesContaining(Role role);

    // ── Pagination (inherited JpaRepository) ─────────────────────────────────
    // findAll(Pageable) is available from JpaRepository — used by UserService.getAllUsers()

    // ── Stats: Count Queries ──────────────────────────────────────────────────

    /** Users who joined within the given date range. */
    long countByJoinedAtBetween(LocalDateTime start, LocalDateTime end);

    /** Users who logged in within the given date range (active users metric). */
    long countByLastLoginBetween(LocalDateTime start, LocalDateTime end);

    // ── Stats: Trend Data (last 7 days) ──────────────────────────────────────

    /**
     * Returns daily user registration counts for the last N days.
     * Each row is [dateString (YYYY-MM-DD), count].
     */
    @Query("""
            SELECT FUNCTION('DATE_FORMAT', u.joinedAt, '%Y-%m-%d'), COUNT(u)
            FROM User u
            WHERE u.joinedAt >= :since
            GROUP BY FUNCTION('DATE_FORMAT', u.joinedAt, '%Y-%m-%d')
            ORDER BY FUNCTION('DATE_FORMAT', u.joinedAt, '%Y-%m-%d')
            """)
    List<Object[]> findUserJoinTrend(@Param("since") LocalDateTime since);

    /**
     * Returns daily active-user counts (last login) for the last N days.
     */
    @Query("""
            SELECT FUNCTION('DATE_FORMAT', u.lastLogin, '%Y-%m-%d'), COUNT(u)
            FROM User u
            WHERE u.lastLogin >= :since
            GROUP BY FUNCTION('DATE_FORMAT', u.lastLogin, '%Y-%m-%d')
            ORDER BY FUNCTION('DATE_FORMAT', u.lastLogin, '%Y-%m-%d')
            """)
    List<Object[]> findActiveUserTrend(@Param("since") LocalDateTime since);

    // ── Stats: Growth Chart (all time) ────────────────────────────────────────

    /**
     * Returns all-time daily user registrations for the growth chart.
     */
    @Query("""
            SELECT FUNCTION('DATE_FORMAT', u.joinedAt, '%Y-%m-%d'), COUNT(u)
            FROM User u
            GROUP BY FUNCTION('DATE_FORMAT', u.joinedAt, '%Y-%m-%d')
            ORDER BY FUNCTION('DATE_FORMAT', u.joinedAt, '%Y-%m-%d')
            """)
    List<Object[]> findUserGrowthByDate();

    // ── Stats: Latest Signups ─────────────────────────────────────────────────

    /** Returns the 5 most recently joined users for the admin dashboard. */
    List<User> findTop5ByOrderByJoinedAtDesc();
}
