package in.skillsync.mentor.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MentorSearchCriteria {

    private Long skillId;
    private BigDecimal minRating;
    private BigDecimal maxHourlyRate;
    private Integer minExperience;

    private int page = 0;
    private int size = 10;
}
