package in.skillsync.mentor.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.exception.UnauthorizedActionException;
import in.skillsync.mentor.dto.MentorApplicationRequest;
import in.skillsync.mentor.dto.MentorProfileResponse;
import in.skillsync.mentor.dto.MentorSearchCriteria;
import in.skillsync.mentor.entity.MentorProfile;
import in.skillsync.mentor.entity.MentorStatus;
import in.skillsync.mentor.repository.MentorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorService {

    private final MentorProfileRepository mentorProfileRepository;

    @Transactional
    public MentorProfileResponse applyAsMentor(Long authUserId,
                                                MentorApplicationRequest request) {
        if (mentorProfileRepository.existsByAuthUserId(authUserId)) {
            throw new UnauthorizedActionException(
                    "Mentor application already exists for user: " + authUserId);
        }

        MentorProfile profile = MentorProfile.builder()
                .authUserId(authUserId)
                .bio(request.getBio())
                .yearsOfExperience(request.getYearsOfExperience())
                .hourlyRate(request.getHourlyRate())
                .skillIds(request.getSkillIds())
                .status(MentorStatus.PENDING_APPROVAL)
                .build();

        MentorProfile saved = mentorProfileRepository.save(profile);
        log.info("Mentor application submitted for authUserId: {}", authUserId);
        return mapToResponse(saved);
    }

    @Cacheable(value = "mentorProfiles", key = "#id")
    public MentorProfileResponse getMentorById(Long id) {
        return mentorProfileRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("MentorProfile", "id", id));
    }

    @Cacheable(value = "mentorProfiles", key = "#authUserId")
    public MentorProfileResponse getMentorByAuthUserId(Long authUserId) {
        return mentorProfileRepository.findByAuthUserId(authUserId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MentorProfile", "authUserId", authUserId));
    }

    public Page<MentorProfileResponse> searchMentors(MentorSearchCriteria criteria) {
        PageRequest pageable = PageRequest.of(
                criteria.getPage(),
                criteria.getSize(),
                Sort.by(Sort.Direction.DESC, "averageRating")
        );

        return mentorProfileRepository.searchActiveMentors(
                criteria.getSkillId(),
                criteria.getMinRating(),
                criteria.getMaxHourlyRate(),
                criteria.getMinExperience(),
                pageable
        ).map(this::mapToResponse);
    }

    public Page<MentorProfileResponse> getPendingMentors(int page, int size) {
        return mentorProfileRepository
                .findByStatus(MentorStatus.PENDING_APPROVAL,
                        PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @CacheEvict(value = "mentorProfiles", key = "#mentorId")
    @Transactional
    public MentorProfileResponse approveMentor(Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MentorProfile", "id", mentorId));

        profile.setStatus(MentorStatus.ACTIVE);
        MentorProfile saved = mentorProfileRepository.save(profile);
        log.info("Mentor approved: mentorId={}", mentorId);
        return mapToResponse(saved);
    }

    @CacheEvict(value = "mentorProfiles", key = "#mentorId")
    @Transactional
    public MentorProfileResponse rejectMentor(Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MentorProfile", "id", mentorId));

        profile.setStatus(MentorStatus.REJECTED);
        MentorProfile saved = mentorProfileRepository.save(profile);
        log.info("Mentor rejected: mentorId={}", mentorId);
        return mapToResponse(saved);
    }

    private MentorProfileResponse mapToResponse(MentorProfile profile) {
        return MentorProfileResponse.builder()
                .id(profile.getId())
                .authUserId(profile.getAuthUserId())
                .bio(profile.getBio())
                .yearsOfExperience(profile.getYearsOfExperience())
                .hourlyRate(profile.getHourlyRate())
                .status(profile.getStatus())
                .averageRating(profile.getAverageRating())
                .skillIds(profile.getSkillIds())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
