package in.skillsync.session.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Core session entity representing a mentoring booking.
 * Table: mentoring_sessions in skillsync_session_db
 *
 * mentorId  → logical FK to mentor_profiles.id  (Mentor Service DB)
 * learnerId → logical FK to auth_users.id        (Auth Service DB)
 * Both IDs come from JWT headers injected by the API Gateway.
 */
@Entity
@Table(name = "mentoring_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentoringSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "learner_id", nullable = false)
    private Long learnerId;

    @Future(message = "Session must be scheduled in the future")
    @NotNull(message = "Session date and time is required")
    @Column(name = "session_date_time", nullable = false)
    private LocalDateTime sessionDateTime;

    @Min(value = 30, message = "Session must be at least 30 minutes")
    @Max(value = 180, message = "Session cannot exceed 180 minutes")
    @NotNull(message = "Duration is required")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Size(max = 500, message = "Topic cannot exceed 500 characters")
    @Column(length = 500)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.REQUESTED;

    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
