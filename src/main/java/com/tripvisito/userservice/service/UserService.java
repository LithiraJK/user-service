package com.tripvisito.userservice.service;

import com.tripvisito.userservice.dto.response.PagedResponse;
import com.tripvisito.userservice.dto.response.UserResponse;
import com.tripvisito.userservice.entity.Role;
import com.tripvisito.userservice.entity.User;
import com.tripvisito.userservice.exception.UserNotFoundException;
import com.tripvisito.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management service.
 *
 * <p>Covers the admin-facing operations originally scattered across
 * {@code auth.controller.ts}: fetching all users (paginated),
 * updating block status, and deleting users.
 * Also provides the {@code getMyProfile} lookup used by
 * {@code GET /api/v1/auth/me}.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    /**
     * Returns a user's own profile.
     * Mirrors: {@code GET /api/v1/auth/me} → {@code getMyProfile()}
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    /**
     * Updates the authenticated user's profile details.
     */
    public UserResponse updateProfile(Long userId, String name, String profileImg) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (profileImg != null && !profileImg.isBlank()) {
            user.setProfileImg(profileImg);
        }

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    // ── Admin: List Users ─────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all users (newest first).
     * Mirrors: {@code GET /api/v1/auth/users} → {@code getAllUsers()}
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public PagedResponse<UserResponse> getAllUsers(int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage = userRepository.findAll(pageRequest);

        List<UserResponse> items = userPage.getContent().stream()
                .map(UserResponse::from)
                .toList();

        return PagedResponse.of(items, userPage.getTotalElements(), page, limit);
    }

    // ── Admin: Update Block Status ────────────────────────────────────────────

    /**
     * Toggles a user's blocked status.
     * Guards against blocking ADMIN users (same check as original code).
     * Mirrors: {@code PUT /api/v1/auth/status/:id} → {@code updateUserStatus()}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public UserResponse updateUserStatus(Long userId, boolean isBlocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("Cannot change status of an Admin user");
        }

        user.setBlocked(isBlocked);
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    // ── Admin: Delete User ────────────────────────────────────────────────────

    /**
     * Deletes a user. Admin accounts cannot be deleted.
     * Mirrors: {@code DELETE /api/v1/auth/delete/:id} → {@code deleteUser()}
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRoles().contains(Role.ADMIN)) {
            throw new RuntimeException("Cannot delete Admin users");
        }

        userRepository.delete(user);
    }

    /**
     * Admin-driven user update. Only callable by ADMIN or SUPERADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public UserResponse adminUpdateUser(Long userId, String name, String email, String password, java.util.Set<Role> roles, String profileImg) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getRoles().contains(Role.SUPERADMIN)) {
            throw new RuntimeException("Cannot modify Superadmin details");
        }

        // Email conflict check
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new com.tripvisito.userservice.exception.EmailAlreadyExistsException("User already exists with this email");
        }

        user.setName(name);
        user.setEmail(email.trim().toLowerCase());
        user.setRoles(roles);

        if (password != null && !password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        if (profileImg != null && !profileImg.isBlank()) {
            user.setProfileImg(profileImg);
        }

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
