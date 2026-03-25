package in.skillsync.user.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.user.dto.UserProfileRequest;
import in.skillsync.user.entity.UserProfile;
import in.skillsync.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService Unit Tests")
class UserProfileServiceTest {

    @Mock private UserProfileRepository userProfileRepository;
    @InjectMocks private UserProfileService userProfileService;

    private UserProfile profile;
    private UserProfileRequest request;

    @BeforeEach
    void setUp() {
        profile = UserProfile.builder()
                .id(1L)
                .authUserId(10L)
                .fullName("Renu Dhankhar")
                .bio("Software Engineer")
                .linkedinUrl("https://linkedin.com/in/renu")
                .build();

        request = new UserProfileRequest();
        request.setFullName("Renu Dhankhar");
        request.setBio("Software Engineer");
        request.setLinkedinUrl("https://linkedin.com/in/renu");
    }

    @Test
    @DisplayName("createProfile - success - returns UserProfileResponse")
    void createProfile_success_returnsResponse() {
        when(userProfileRepository.save(any())).thenReturn(profile);

        var response = userProfileService.createProfile(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.getAuthUserId()).isEqualTo(10L);
        assertThat(response.getFullName()).isEqualTo("Renu Dhankhar");
        verify(userProfileRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("getProfileByAuthUserId - profile exists - returns response")
    void getProfileByAuthUserId_exists_returnsResponse() {
        when(userProfileRepository.findByAuthUserId(10L)).thenReturn(Optional.of(profile));

        var response = userProfileService.getProfileByAuthUserId(10L);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Renu Dhankhar");
    }

    @Test
    @DisplayName("getProfileByAuthUserId - not found - throws ResourceNotFoundException")
    void getProfileByAuthUserId_notFound_throwsException() {
        when(userProfileRepository.findByAuthUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfileByAuthUserId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
