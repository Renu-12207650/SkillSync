package in.skillsync.session.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Optional rejection reason when mentor rejects a session.
 */
@Data
public class RejectSessionRequest {

    @Size(max = 500, message = "Rejection reason cannot exceed 500 characters")
    private String rejectionReason;
}
