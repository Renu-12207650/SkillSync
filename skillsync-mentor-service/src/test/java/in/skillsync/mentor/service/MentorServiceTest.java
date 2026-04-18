package in.skillsync.mentor.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.exception.UnauthorizedActionException;
import in.skillsync.mentor.dto.MentorApplicationRequest;
import in.skillsync.mentor.dto.MentorProfileResponse;
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
import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    private MentorService mentorService;

    @Mock
    private MentorProfileRepository repo;

    private MentorProfile profile;

    @BeforeEach
    void setup() {
        profile = MentorProfile.builder()
                .id(1L)
                .authUserId(10L)
                .bio("bio")
                .yearsOfExperience(5)
                .hourlyRate(BigDecimal.valueOf(100.0)) // ✅ FIXED
                .status(MentorStatus.PENDING_APPROVAL)
                .build();
    }

    @Test
    void applyMentor_success() {
        MentorApplicationRequest req = new MentorApplicationRequest();

        when(repo.existsByAuthUserId(any())).thenReturn(false);
        when(repo.save(any())).thenReturn(profile);

        MentorProfileResponse res = mentorService.applyAsMentor(10L, req);

        assertNotNull(res);
    }

    @Test
    void applyMentor_alreadyExists() {
        when(repo.existsByAuthUserId(any())).thenReturn(true);

        assertThrows(UnauthorizedActionException.class,
                () -> mentorService.applyAsMentor(10L, new MentorApplicationRequest()));
    }

    @Test
    void getMentorById_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(profile));

        assertNotNull(mentorService.getMentorById(1L));
    }

    @Test
    void getMentorById_notFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> mentorService.getMentorById(1L));
    }

    @Test
    void approveMentor_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(profile));
        when(repo.save(any())).thenReturn(profile);

        MentorProfileResponse res = mentorService.approveMentor(1L);

        assertEquals(MentorStatus.ACTIVE, profile.getStatus());
    }

    @Test
    void rejectMentor_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(profile));
        when(repo.save(any())).thenReturn(profile);

        MentorProfileResponse res = mentorService.rejectMentor(1L);

        assertEquals(MentorStatus.REJECTED, profile.getStatus());
    }
}