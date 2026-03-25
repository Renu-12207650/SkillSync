package in.skillsync.mentor.dto;

import in.skillsync.mentor.entity.MentorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorProfileResponse {

    private Long id;
    private Long authUserId;
    private String bio;
    private Integer yearsOfExperience;
    private BigDecimal hourlyRate;
    private MentorStatus status;
    private BigDecimal averageRating;
    private Set<Long> skillIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
