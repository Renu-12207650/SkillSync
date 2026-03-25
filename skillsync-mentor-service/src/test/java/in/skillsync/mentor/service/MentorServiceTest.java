package in.skillsync.mentor.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.exception.UnauthorizedActionException;
import in.skillsync.mentor.dto.MentorApplicationRequest;
import in.skillsync.mentor.entity.MentorProfile;
import in.skillsync.mentor.entity.MentorStatus;
import in.skillsync.mentor.repository.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorService Unit Tests")
class MentorServiceTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @InjectMocks private MentorService mentorService;

    private MentorApplicationRequest applicationRequest;
    private MentorProfile savedProfile;

    @BeforeEach
    void setUp() {
        applicationRequest = new MentorApplicationRequest();
        applicationRequest.setBio("Experienced Java Developer with 5 years in Spring Boot");
        applicationRequest.setYearsOfExperience(5);
        applicationRequest.setHourlyRate(new BigDecimal("500.00"));
        applicationRequest.setSkillIds(Set.of(1L, 2L));

        savedProfile = MentorProfile.builder()
                .id(1L)
                .authUserId(10L)
                .bio("Experienced Java Developer with 5 years in Spring Boot")
                .yearsOfExperience(5)
                .hourlyRate(new BigDecimal("500.00"))
                .status(MentorStatus.PENDING_APPROVAL)
                .averageRating(BigDecimal.ZERO)
                .skillIds(Set.of(1L, 2L))
                .build();
    }

    @Test
    @DisplayName("applyAsMentor - success - returns response with PENDING_APPROVAL status")
    void applyAsMentor_success_returnsPendingApprovalStatus() {
        when(mentorProfileRepository.existsByAuthUserId(10L)).thenReturn(false);
        when(mentorProfileRepository.save(any())).thenReturn(savedProfile);

        var response = mentorService.applyAsMentor(10L, applicationRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(MentorStatus.PENDING_APPROVAL);
        assertThat(response.getYearsOfExperience()).isEqualTo(5);
        verify(mentorProfileRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("applyAsMentor - duplicate - throws UnauthorizedActionException")
    void applyAsMentor_duplicate_throwsException() {
        when(mentorProfileRepository.existsByAuthUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> mentorService.applyAsMentor(10L, applicationRequest))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(mentorProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveMentor - success - status changes to ACTIVE")
    void approveMentor_success_statusBecomesActive() {
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(savedProfile));
        when(mentorProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = mentorService.approveMentor(1L);

        assertThat(response.getStatus()).isEqualTo(MentorStatus.ACTIVE);
    }

    @Test
    @DisplayName("getMentorById - not found - throws ResourceNotFoundException")
    void getMentorById_notFound_throwsResourceNotFoundException() {
        when(mentorProfileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mentorService.getMentorById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
