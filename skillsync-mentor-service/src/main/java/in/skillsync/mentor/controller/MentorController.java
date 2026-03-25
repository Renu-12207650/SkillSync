package in.skillsync.mentor.controller;

import in.skillsync.mentor.dto.MentorApplicationRequest;
import in.skillsync.mentor.dto.MentorProfileResponse;
import in.skillsync.mentor.dto.MentorSearchCriteria;
import in.skillsync.mentor.service.MentorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for mentor operations.
 * Base path: /mentors
 */
@RestController
@RequestMapping("/mentors")
@RequiredArgsConstructor
@Tag(name = "Mentors", description = "Mentor onboarding, discovery and admin approval")
@SecurityRequirement(name = "bearerAuth")
public class MentorController {

    private final MentorService mentorService;

    @PostMapping("/apply")
    @Operation(summary = "Apply to become a mentor (ROLE_LEARNER)")
    public ResponseEntity<MentorProfileResponse> applyAsMentor(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody MentorApplicationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mentorService.applyAsMentor(authUserId, request));
    }

    @GetMapping
    @Operation(summary = "Search active mentors with optional filters")
    public ResponseEntity<Page<MentorProfileResponse>> searchMentors(
            MentorSearchCriteria criteria) {
        return ResponseEntity.ok(mentorService.searchMentors(criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get mentor profile by ID")
    public ResponseEntity<MentorProfileResponse> getMentorById(@PathVariable Long id) {
        return ResponseEntity.ok(mentorService.getMentorById(id));
    }

    @GetMapping("/my-profile")
    @Operation(summary = "Get my own mentor profile")
    public ResponseEntity<MentorProfileResponse> getMyMentorProfile(
            @RequestHeader("X-User-Id") Long authUserId) {
        return ResponseEntity.ok(mentorService.getMentorByAuthUserId(authUserId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get all pending mentor applications (ROLE_ADMIN)")
    public ResponseEntity<Page<MentorProfileResponse>> getPendingMentors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mentorService.getPendingMentors(page, size));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Approve a mentor application (ROLE_ADMIN)")
    public ResponseEntity<MentorProfileResponse> approveMentor(@PathVariable Long id) {
        return ResponseEntity.ok(mentorService.approveMentor(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Reject a mentor application (ROLE_ADMIN)")
    public ResponseEntity<MentorProfileResponse> rejectMentor(@PathVariable Long id) {
        return ResponseEntity.ok(mentorService.rejectMentor(id));
    }
}
