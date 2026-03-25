package in.skillsync.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event payload received from RabbitMQ.
 * Published by Session Service, Mentor Service, etc.
 * The eventType field determines which handler is invoked.
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

    /**
     * Possible values:
     * SESSION_BOOKED, SESSION_ACCEPTED, SESSION_REJECTED,
     * SESSION_COMPLETED, MENTOR_APPROVED, MENTOR_REJECTED
     */
    private String eventType;
}
