package in.skillsync.mentor.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Mentor profile entity.
 * Linked to AuthUser via authUserId (logical cross-service FK).
 * Table: mentor_profiles in skillsync_mentor_db
 */
@Entity
@Table(
    name = "mentor_profiles",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "auth_user_id", name = "uk_mentor_profiles_auth_user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth_user_id", nullable = false, unique = true)
    private Long authUserId;

    @NotBlank(message = "Bio is required")
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String bio;

    @Min(value = 0, message = "Years of experience must be non-negative")
    @Max(value = 50, message = "Years of experience cannot exceed 50")
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MentorStatus status = MentorStatus.PENDING_APPROVAL;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    /**
     * Stores IDs referencing skills in Skill Service.
     * Logical cross-service FK — no DB-level constraint.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "mentor_skill_ids",
        joinColumns = @JoinColumn(name = "mentor_id")
    )
    @Column(name = "skill_id")
    @Builder.Default
    private Set<Long> skillIds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
