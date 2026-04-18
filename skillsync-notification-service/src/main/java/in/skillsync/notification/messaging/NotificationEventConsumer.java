package in.skillsync.notification.messaging;

import in.skillsync.notification.client.AuthClient;
import in.skillsync.notification.config.RabbitMQConfig;
import in.skillsync.notification.dto.SessionEventPayload;
import in.skillsync.notification.service.EmailService;
import in.skillsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final AuthClient authClient; 
    private final EmailService emailService; 

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleEvent(SessionEventPayload payload) {
        log.info("Received event: {} for session: {}", payload.getEventType(), payload.getSessionId());

        if (payload.getEventType() == null) {
            log.warn("Received event with null eventType — skipping");
            return;
        }

        switch (payload.getEventType()) {
            case "SESSION_BOOKED" -> {
                notificationService.handleSessionBooked(payload);
                sendSessionEmail(payload, "Your session has been successfully booked!");
            }
            case "SESSION_ACCEPTED" -> {
                notificationService.handleSessionAccepted(payload);
                sendSessionEmail(payload, "Great news! Your session was accepted by the mentor.");
            }
            case "SESSION_REJECTED" -> {
                notificationService.handleSessionRejected(payload);
                sendSessionEmail(payload, "We're sorry, your session request was declined.");
            }
            case "SESSION_COMPLETED" -> notificationService.handleSessionCompleted(payload);
            case "MENTOR_APPROVED"   -> notificationService.handleMentorApproved(payload);
            default -> log.warn("Unhandled event type: {}", payload.getEventType());
        }
    }

    private void sendSessionEmail(SessionEventPayload payload, String message) {
        try {
            String userEmail = authClient.getUserEmail(payload.getLearnerId());
            if (userEmail != null) {
                emailService.sendEmail(userEmail, "SkillSync Update: " + payload.getEventType(), message);
            }
        } catch (Exception e) {
            log.error("Could not send email notification: {}", e.getMessage());
        }
    }
}