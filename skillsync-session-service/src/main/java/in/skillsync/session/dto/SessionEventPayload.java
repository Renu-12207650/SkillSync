package in.skillsync.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event payload published to RabbitMQ on every session state transition.
 * Consumed by Notification Service.
 *
 * eventType values:
 * SESSION_BOOKED, SESSION_ACCEPTED, SESSION_REJECTED,
 * SESSION_COMPLETED, SESSION_CANCELLED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEventPayload implements Serializable {

    private Long sessionId;
    private Long mentorId;
    private Long learnerId;
    private LocalDateTime sessionDateTime;
    private String topic;
    private String eventType;
    private String rejectionReason;
}
