package in.skillsync.session.dto;

import in.skillsync.session.entity.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO returned for session queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private Long id;
    private Long mentorId;
    private Long learnerId;
    private LocalDateTime sessionDateTime;
    private Integer durationMinutes;
    private String topic;
    private SessionStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
