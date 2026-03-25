package in.skillsync.session.messaging;

import in.skillsync.session.config.RabbitMQConfig;
import in.skillsync.session.dto.SessionEventPayload;
import in.skillsync.session.entity.MentoringSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes session lifecycle events to RabbitMQ TopicExchange.
 * Each event carries the full SessionEventPayload so the
 * Notification Service does not need to make additional API calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishSessionBooked(MentoringSession session) {
        publish(session, RabbitMQConfig.ROUTING_SESSION_BOOKED, "SESSION_BOOKED", null);
    }

    public void publishSessionAccepted(MentoringSession session) {
        publish(session, RabbitMQConfig.ROUTING_SESSION_ACCEPTED, "SESSION_ACCEPTED", null);
    }

    public void publishSessionRejected(MentoringSession session) {
        publish(session, RabbitMQConfig.ROUTING_SESSION_REJECTED, "SESSION_REJECTED",
                session.getRejectionReason());
    }

    public void publishSessionCompleted(MentoringSession session) {
        publish(session, RabbitMQConfig.ROUTING_SESSION_COMPLETED, "SESSION_COMPLETED", null);
    }

    public void publishSessionCancelled(MentoringSession session) {
        publish(session, RabbitMQConfig.ROUTING_SESSION_CANCELLED, "SESSION_CANCELLED", null);
    }

    private void publish(MentoringSession session, String routingKey,
                         String eventType, String rejectionReason) {
        SessionEventPayload payload = SessionEventPayload.builder()
                .sessionId(session.getId())
                .mentorId(session.getMentorId())
                .learnerId(session.getLearnerId())
                .sessionDateTime(session.getSessionDateTime())
                .topic(session.getTopic())
                .eventType(eventType)
                .rejectionReason(rejectionReason)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, payload);
        log.info("Published event: {} for sessionId: {}", eventType, session.getId());
    }
}
