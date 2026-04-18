package in.skillsync.notification.service;

import in.skillsync.notification.dto.NotificationResponse;
import in.skillsync.notification.dto.SessionEventPayload;
import in.skillsync.notification.entity.Notification;
import in.skillsync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles notification creation (in-app) and email delivery.
 * Invoked by NotificationEventConsumer when RabbitMQ events arrive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    // ── Event Handlers ──────────────────────────────────────────────────────

    @Transactional
    public void handleSessionBooked(SessionEventPayload payload) {
        // Notify mentor: new session request
        saveAndNotify(
                payload.getMentorId(),
                "SESSION_REQUEST",
                "New Session Request",
                "You have a new session request for topic: " + payload.getTopic(),
                null
        );
        // Confirm to learner
        saveAndNotify(
                payload.getLearnerId(),
                "SESSION_BOOKED",
                "Session Request Sent",
                "Your session request has been sent successfully. Waiting for mentor confirmation.",
                null
        );
    }

    @Transactional
    public void handleSessionAccepted(SessionEventPayload payload) {
        saveAndNotify(
                payload.getLearnerId(),
                "SESSION_ACCEPTED",
                "Session Accepted!",
                "Great news! Your session for \"" + payload.getTopic() + "\" has been accepted.",
                null
        );
    }

    @Transactional
    public void handleSessionRejected(SessionEventPayload payload) {
        saveAndNotify(
                payload.getLearnerId(),
                "SESSION_REJECTED",
                "Session Request Rejected",
                "Unfortunately your session request was rejected. Please try booking with another mentor.",
                null
        );
    }

    @Transactional
    public void handleSessionCompleted(SessionEventPayload payload) {
        saveAndNotify(
                payload.getLearnerId(),
                "SESSION_COMPLETED",
                "Session Complete — Leave a Review!",
                "Your session has been completed. Please take a moment to rate your mentor.",
                null
        );
    }

    @Transactional
    public void handleMentorApproved(SessionEventPayload payload) {
        saveAndNotify(
                payload.getMentorId(),
                "MENTOR_APPROVED",
                "Congratulations! Your Mentor Application is Approved",
                "Your mentor profile is now active. Learners can discover and book sessions with you.",
                null
        );
    }

    // ── Query Methods ────────────────────────────────────────────────────────

    public List<NotificationResponse> getAllNotifications(Long userId) {
        return notificationRepository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException(
                        "Notification not found: " + notificationId));
        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void saveAndNotify(Long userId, String type,
                                String title, String message,
                                String recipientEmail) {
        // Persist in-app notification
        notificationRepository.save(
                Notification.builder()
                        .recipientUserId(userId)
                        .type(type)
                        .title(title)
                        .message(message)
                        .read(false)
                        .build()
        );

        // Send email if address provided
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            sendEmail(recipientEmail, title, message);
        }

        log.info("Notification saved — userId: {}, type: {}", userId, type);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("renudhankhar8559@gmail.com");
            mail.setTo(to);
            mail.setSubject("[SkillSync] " + subject);
            mail.setText(body);
            mailSender.send(mail);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .recipientUserId(n.getRecipientUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
