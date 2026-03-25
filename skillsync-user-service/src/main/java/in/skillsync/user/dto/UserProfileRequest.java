package in.skillsync.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private String profileImageUrl;
    private String linkedinUrl;
    private String githubUrl;
}
