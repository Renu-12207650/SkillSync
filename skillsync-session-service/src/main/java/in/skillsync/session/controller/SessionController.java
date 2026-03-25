package in.skillsync.session.controller;

import in.skillsync.session.dto.RejectSessionRequest;
import in.skillsync.session.dto.SessionRequest;
import in.skillsync.session.dto.SessionResponse;
import in.skillsync.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for session lifecycle management.
 * Base path: /sessions
 *
 * All endpoints require JWT (validated by API Gateway).
 * X-User-Id header is injected by Gateway — no manual token parsing needed.
 */
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions", description = "Mentoring session booking and lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(
        summary = "Book a session (ROLE_LEARNER)",
        description = "Learner books a session with a mentor. Status becomes REQUESTED. " +
                      "RabbitMQ event SESSION_BOOKED is published."
    )
    public ResponseEntity<SessionResponse> bookSession(
            @RequestHeader("X-User-Id") Long learnerId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(sessionService.bookSession(learnerId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get session by ID")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSessionById(id));
    }

    @GetMapping("/my/as-learner")
    @Operation(summary = "Get all my sessions as a learner")
    public ResponseEntity<Page<SessionResponse>> getMySessionsAsLearner(
            @RequestHeader("X-User-Id") Long learnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                sessionService.getMySessionsAsLearner(learnerId, page, size));
    }

    @GetMapping("/my/as-mentor")
    @Operation(summary = "Get all my sessions as a mentor")
    public ResponseEntity<Page<SessionResponse>> getMySessionsAsMentor(
            @RequestHeader("X-User-Id") Long mentorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                sessionService.getMySessionsAsMentor(mentorId, page, size));
    }

    @PutMapping("/{id}/accept")
    @Operation(
        summary = "Accept a session request (ROLE_MENTOR)",
        description = "Mentor accepts a REQUESTED session. " +
                      "Status → ACCEPTED. RabbitMQ event SESSION_ACCEPTED published."
    )
    public ResponseEntity<SessionResponse> acceptSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long mentorId) {
        return ResponseEntity.ok(sessionService.acceptSession(id, mentorId));
    }

    @PutMapping("/{id}/reject")
    @Operation(
        summary = "Reject a session request (ROLE_MENTOR)",
        description = "Mentor rejects a REQUESTED session with optional reason. " +
                      "Status → REJECTED. RabbitMQ event SESSION_REJECTED published."
    )
    public ResponseEntity<SessionResponse> rejectSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long mentorId,
            @RequestBody(required = false) RejectSessionRequest request) {
        return ResponseEntity.ok(sessionService.rejectSession(id, mentorId, request));
    }

    @PutMapping("/{id}/cancel")
    @Operation(
        summary = "Cancel a session (Learner or Mentor)",
        description = "Either party can cancel a REQUESTED or ACCEPTED session. " +
                      "Status → CANCELLED. RabbitMQ event SESSION_CANCELLED published."
    )
    public ResponseEntity<SessionResponse> cancelSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(sessionService.cancelSession(id, userId));
    }

    @PutMapping("/{id}/complete")
    @Operation(
        summary = "Mark session as completed (ROLE_MENTOR)",
        description = "Mentor marks an ACCEPTED session as completed. " +
                      "Status → COMPLETED. RabbitMQ event SESSION_COMPLETED published."
    )
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long mentorId) {
        return ResponseEntity.ok(sessionService.completeSession(id, mentorId));
    }
}
