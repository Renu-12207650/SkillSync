package in.skillsync.session.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request DTO for booking a new mentoring session.
 */
@Data
public class SessionRequest {

    @NotNull(message = "Mentor ID is required")
    private Long mentorId;

    @NotNull(message = "Session date and time is required")
    @Future(message = "Session must be scheduled in the future")
    private LocalDateTime sessionDateTime;

    @NotNull(message = "Duration is required")
    @Min(value = 30, message = "Session must be at least 30 minutes")
    @Max(value = 180, message = "Session cannot exceed 180 minutes")
    private Integer durationMinutes;

    @Size(max = 500, message = "Topic cannot exceed 500 characters")
    private String topic;
}
