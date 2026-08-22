package com.tripvisito.userservice.controller;

import com.tripvisito.userservice.dto.request.*;
import com.tripvisito.userservice.dto.response.ApiResponse;
import com.tripvisito.userservice.dto.response.AuthResponse;
import com.tripvisito.userservice.dto.response.PagedResponse;
import com.tripvisito.userservice.dto.response.UserResponse;
import com.tripvisito.userservice.service.AuthService;
import com.tripvisito.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripvisito.userservice.entity.Role;
import com.tripvisito.userservice.service.GcpStorageService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;

/**
 * REST controller for all authentication and user management endpoints.
 *
 * <p>Consolidates the original Express {@code auth.routes.ts} into a single
 * Spring MVC controller. Routes are structured to match the original
 * {@code /api/v1/auth/**} prefix, which the api-gateway routes to this service.
 *
 * <p>User identity on protected routes is extracted from the {@code X-User-Id}
 * header (injected by the gateway) rather than parsing a JWT directly.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final GcpStorageService gcpStorageService;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService,
                          UserService userService,
                          GcpStorageService gcpStorageService,
                          Validator validator,
                          ObjectMapper objectMapper) {
        this.authService = authService;
        this.userService = userService;
        this.gcpStorageService = gcpStorageService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    // ── Public Endpoints ─────────────────────────────────────────────────────

    /** POST /api/v1/auth/register */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "User registered successfully", user));
    }

    /** POST /api/v1/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User logged in successfully", response));
    }

    /** POST /api/v1/auth/google-login */
    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
    }

    /** POST /api/v1/auth/refresh */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        String newAccessToken = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", newAccessToken));
    }

    // ── Authenticated Endpoints ───────────────────────────────────────────────

    /**
     * GET /api/v1/auth/me
     * Reads userId from the {@code X-User-Id} header injected by the gateway.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        UserResponse profile = userService.getMyProfile(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success("ok", profile));
    }

    /**
     * PUT /api/v1/auth/profile
     * Updates the authenticated user's own profile (name, optional profile image).
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("name") String name,
            @RequestParam(value = "profileimg", required = false) MultipartFile profileimg) throws Exception {

        String profileImgUrl = null;
        if (profileimg != null && !profileimg.isEmpty()) {
            try {
                profileImgUrl = gcpStorageService.uploadFile(
                        profileimg.getOriginalFilename(),
                        profileimg.getBytes(),
                        profileimg.getContentType()
                );
            } catch (Exception e) {
                System.err.println("[AuthController] GCP Profile Update Upload failed: " + e.getMessage());
            }
        }

        UserResponse updated = userService.updateProfile(Long.parseLong(userId), name, profileImgUrl);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updated));
    }

    // ── Admin Endpoints ───────────────────────────────────────────────────────
    // Role enforcement via @PreAuthorize in UserService / AuthService

    /** POST /api/v1/auth/register/admin — SUPERADMIN only */
    @PostMapping("/register/admin")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAdmin(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Admin created successfully!", response));
    }

    /** GET /api/v1/auth/users?page=1&limit=4 — ADMIN / SUPERADMIN */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "4") int limit) {
        PagedResponse<UserResponse> result = userService.getAllUsers(page, limit);
        return ResponseEntity.ok(ApiResponse.success("Users Data fetch successfully !", result));
    }

    /** POST /api/v1/auth/register/new-user — ADMIN / SUPERADMIN */
    @PostMapping(value = "/register/new-user", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> addNewUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("roles") String rolesJson,
            @RequestParam(value = "profileimg", required = false) MultipartFile profileimg) throws Exception {

        Set<Role> roles = new java.util.HashSet<>();
        if (rolesJson != null && !rolesJson.isBlank()) {
            List<String> roleStrings = objectMapper.readValue(rolesJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            for (String r : roleStrings) {
                roles.add(Role.valueOf(r.toUpperCase().trim()));
            }
        }

        String profileImgUrl = null;
        if (profileimg != null && !profileimg.isEmpty()) {
            try {
                profileImgUrl = gcpStorageService.uploadFile(
                        profileimg.getOriginalFilename(),
                        profileimg.getBytes(),
                        profileimg.getContentType()
                );
            } catch (Exception e) {
                System.err.println("[AuthController] GCP Storage upload failed: " + e.getMessage() + ". Falling back to default avatar.");
            }
        }

        AddUserRequest request = new AddUserRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setRoles(roles);
        request.setProfileImgUrl(profileImgUrl);

        Set<ConstraintViolation<AddUserRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        UserResponse user = authService.addNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "User added successfully", user));
    }

    /** PUT /api/v1/auth/status/{id} — ADMIN / SUPERADMIN */
    @PutMapping("/status/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserResponse user = userService.updateUserStatus(id, request.getIsBlocked());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    /** PUT /api/v1/auth/users/edit/{id} — ADMIN / SUPERADMIN */
    @PutMapping(value = "/users/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> adminUpdateUser(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam("roles") String rolesJson,
            @RequestParam(value = "profileimg", required = false) MultipartFile profileimg) throws Exception {

        Set<Role> roles = new java.util.HashSet<>();
        if (rolesJson != null && !rolesJson.isBlank()) {
            List<String> roleStrings = objectMapper.readValue(rolesJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            for (String r : roleStrings) {
                roles.add(Role.valueOf(r.toUpperCase().trim()));
            }
        }

        String profileImgUrl = null;
        if (profileimg != null && !profileimg.isEmpty()) {
            try {
                profileImgUrl = gcpStorageService.uploadFile(
                        profileimg.getOriginalFilename(),
                        profileimg.getBytes(),
                        profileimg.getContentType()
                );
            } catch (Exception e) {
                System.err.println("[AuthController] GCP Profile Update Upload failed: " + e.getMessage());
            }
        }

        UserResponse user = userService.adminUpdateUser(id, name, email, password, roles, profileImgUrl);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    /** DELETE /api/v1/auth/delete/{id} — ADMIN / SUPERADMIN */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}
