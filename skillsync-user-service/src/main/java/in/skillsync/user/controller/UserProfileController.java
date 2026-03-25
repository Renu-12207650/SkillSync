package in.skillsync.user.controller;

import in.skillsync.user.dto.UserProfileRequest;
import in.skillsync.user.dto.UserProfileResponse;
import in.skillsync.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user profile management.
 * All endpoints require JWT authentication (validated by API Gateway).
 * Base path: /users
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profiles", description = "Manage user profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    @Operation(summary = "Create a new user profile")
    public ResponseEntity<UserProfileResponse> createProfile(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userProfileService.createProfile(authUserId, request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my own profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("X-User-Id") Long authUserId) {
        return ResponseEntity.ok(userProfileService.getProfileByAuthUserId(authUserId));
    }

    @GetMapping("/{authUserId}")
    @Operation(summary = "Get profile by auth user ID")
    public ResponseEntity<UserProfileResponse> getProfileByAuthUserId(
            @PathVariable Long authUserId) {
        return ResponseEntity.ok(userProfileService.getProfileByAuthUserId(authUserId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my own profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(authUserId, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get all user profiles (Admin only)")
    public ResponseEntity<List<UserProfileResponse>> getAllProfiles() {
        return ResponseEntity.ok(userProfileService.getAllProfiles());
    }
}
