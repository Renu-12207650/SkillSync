package in.skillsync.session.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.exception.UnauthorizedActionException;
import in.skillsync.session.dto.RejectSessionRequest;
import in.skillsync.session.dto.SessionRequest;
import in.skillsync.session.dto.SessionResponse;
import in.skillsync.session.entity.MentoringSession;
import in.skillsync.session.entity.SessionStatus;
import in.skillsync.session.messaging.SessionEventPublisher;
import in.skillsync.session.repository.MentoringSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Session business logic service.
 * Handles all session state transitions and publishes
 * corresponding RabbitMQ events after each transition.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final MentoringSessionRepository sessionRepository;
    private final SessionEventPublisher eventPublisher;

    // ── Book a session ────────────────────────────────────────────────────────

    @Transactional
    public SessionResponse bookSession(Long learnerId, SessionRequest request) {
        MentoringSession session = MentoringSession.builder()
                .mentorId(request.getMentorId())
                .learnerId(learnerId)
                .sessionDateTime(request.getSessionDateTime())
                .durationMinutes(request.getDurationMinutes())
                .topic(request.getTopic())
                .status(SessionStatus.REQUESTED)
                .build();

        MentoringSession saved = sessionRepository.save(session);
        eventPublisher.publishSessionBooked(saved);
        log.info("Session booked: id={}, mentor={}, learner={}",
                saved.getId(), saved.getMentorId(), saved.getLearnerId());
        return mapToResponse(saved);
    }

    // ── Accept (mentor only) ──────────────────────────────────────────────────

    @Transactional
    public SessionResponse acceptSession(Long sessionId, Long mentorId) {
        MentoringSession session = findById(sessionId);
        validateMentorOwnership(session, mentorId);
        validateStatus(session, SessionStatus.REQUESTED,
                "Only REQUESTED sessions can be accepted");

        session.setStatus(SessionStatus.ACCEPTED);
        MentoringSession saved = sessionRepository.save(session);
        eventPublisher.publishSessionAccepted(saved);
        log.info("Session accepted: id={}", sessionId);
        return mapToResponse(saved);
    }

    // ── Reject (mentor only) ──────────────────────────────────────────────────

    @Transactional
    public SessionResponse rejectSession(Long sessionId, Long mentorId,
                                          RejectSessionRequest request) {
        MentoringSession session = findById(sessionId);
        validateMentorOwnership(session, mentorId);
        validateStatus(session, SessionStatus.REQUESTED,
                "Only REQUESTED sessions can be rejected");

        session.setStatus(SessionStatus.REJECTED);
        if (request != null && request.getRejectionReason() != null) {
            session.setRejectionReason(request.getRejectionReason());
        }
        MentoringSession saved = sessionRepository.save(session);
        eventPublisher.publishSessionRejected(saved);
        log.info("Session rejected: id={}", sessionId);
        return mapToResponse(saved);
    }

    // ── Cancel (learner or mentor) ────────────────────────────────────────────

    @Transactional
    public SessionResponse cancelSession(Long sessionId, Long userId) {
        MentoringSession session = findById(sessionId);

        // Either the learner or the mentor can cancel
        if (!session.getLearnerId().equals(userId)
                && !session.getMentorId().equals(userId)) {
            throw new UnauthorizedActionException(
                    "You are not authorized to cancel this session");
        }

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.REJECTED
                || session.getStatus() == SessionStatus.CANCELLED) {
            throw new UnauthorizedActionException(
                    "Session in status " + session.getStatus() + " cannot be cancelled");
        }

        session.setStatus(SessionStatus.CANCELLED);
        MentoringSession saved = sessionRepository.save(session);
        eventPublisher.publishSessionCancelled(saved);
        log.info("Session cancelled: id={}", sessionId);
        return mapToResponse(saved);
    }

    // ── Complete (mentor only) ────────────────────────────────────────────────

    @Transactional
    public SessionResponse completeSession(Long sessionId, Long mentorId) {
        MentoringSession session = findById(sessionId);
        validateMentorOwnership(session, mentorId);
        validateStatus(session, SessionStatus.ACCEPTED,
                "Only ACCEPTED sessions can be marked as completed");

        session.setStatus(SessionStatus.COMPLETED);
        MentoringSession saved = sessionRepository.save(session);
        eventPublisher.publishSessionCompleted(saved);
        log.info("Session completed: id={}", sessionId);
        return mapToResponse(saved);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public SessionResponse getSessionById(Long sessionId) {
        return mapToResponse(findById(sessionId));
    }

    public Page<SessionResponse> getMySessionsAsLearner(Long learnerId,
                                                         int page, int size) {
        return sessionRepository
                .findByLearnerId(learnerId,
                        PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    public Page<SessionResponse> getMySessionsAsMentor(Long mentorId,
                                                        int page, int size) {
        return sessionRepository
                .findByMentorId(mentorId,
                        PageRequest.of(page, size,
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::mapToResponse);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private MentoringSession findById(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MentoringSession", "id", sessionId));
    }

    private void validateMentorOwnership(MentoringSession session, Long mentorId) {
        if (!session.getMentorId().equals(mentorId)) {
            throw new UnauthorizedActionException(
                    "You are not the mentor for this session");
        }
    }

    private void validateStatus(MentoringSession session,
                                 SessionStatus expected, String message) {
        if (session.getStatus() != expected) {
            throw new UnauthorizedActionException(message +
                    ". Current status: " + session.getStatus());
        }
    }

    private SessionResponse mapToResponse(MentoringSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .mentorId(session.getMentorId())
                .learnerId(session.getLearnerId())
                .sessionDateTime(session.getSessionDateTime())
                .durationMinutes(session.getDurationMinutes())
                .topic(session.getTopic())
                .status(session.getStatus())
                .rejectionReason(session.getRejectionReason())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
