package in.skillsync.mentor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class MentorApplicationRequest {

    @NotBlank(message = "Bio is required")
    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    private String bio;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience must be non-negative")
    @Max(value = 50, message = "Years of experience cannot exceed 50")
    private Integer yearsOfExperience;

    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Hourly rate must be positive")
    private BigDecimal hourlyRate;

    @NotEmpty(message = "At least one skill is required")
    private Set<Long> skillIds;
}
