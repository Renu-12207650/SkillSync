package in.skillsync.notification.messaging;

import in.skillsync.notification.config.RabbitMQConfig;
import in.skillsync.notification.dto.SessionEventPayload;
import in.skillsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event consumer.
 * Listens on skillsync.notification.queue and routes events
 * to the appropriate NotificationService handler based on eventType.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleEvent(SessionEventPayload payload) {
        log.info("Received event: {} for session: {}",
                payload.getEventType(), payload.getSessionId());

        if (payload.getEventType() == null) {
            log.warn("Received event with null eventType — skipping");
            return;
        }

        switch (payload.getEventType()) {
            case "SESSION_BOOKED"    -> notificationService.handleSessionBooked(payload);
            case "SESSION_ACCEPTED"  -> notificationService.handleSessionAccepted(payload);
            case "SESSION_REJECTED"  -> notificationService.handleSessionRejected(payload);
            case "SESSION_COMPLETED" -> notificationService.handleSessionCompleted(payload);
            case "MENTOR_APPROVED"   -> notificationService.handleMentorApproved(payload);
            default -> log.warn("Unhandled event type: {}", payload.getEventType());
        }
    }
}
