package in.skillsync.user.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.user.dto.UserProfileRequest;
import in.skillsync.user.dto.UserProfileResponse;
import in.skillsync.user.entity.UserProfile;
import in.skillsync.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for managing User Profiles.
 * Optimized for SonarCloud standards and Java 17.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    // Fix for SonarCloud "Duplicate Literal" - Defines the resource name once
    private static final String RESOURCE_NAME = "UserProfile";

    @Transactional
    public UserProfileResponse createProfile(Long authUserId, UserProfileRequest request) {
        UserProfile profile = UserProfile.builder()
                .authUserId(authUserId)
                .fullName(request.getFullName())
                .bio(request.getBio())
                .profileImageUrl(request.getProfileImageUrl())
                .linkedinUrl(request.getLinkedinUrl())
                .githubUrl(request.getGithubUrl())
                .build();

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Created profile for authUserId: {}", authUserId);
        return mapToResponse(saved);
    }

    @Cacheable(value = "userProfiles", key = "#authUserId")
    public UserProfileResponse getProfileByAuthUserId(Long authUserId) {
        log.debug("Fetching profile for authUserId: {}", authUserId);
        return userProfileRepository.findByAuthUserId(authUserId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "authUserId", authUserId));
    }

    @Cacheable(value = "userProfiles", key = "#id")
    public UserProfileResponse getProfileById(Long id) {
        return userProfileRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));
    }

    @CacheEvict(value = "userProfiles", key = "#authUserId")
    @Transactional
    public UserProfileResponse updateProfile(Long authUserId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "authUserId", authUserId));

        profile.setFullName(request.getFullName());
        profile.setBio(request.getBio());
        profile.setProfileImageUrl(request.getProfileImageUrl());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setGithubUrl(request.getGithubUrl());

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Updated profile for authUserId: {}", authUserId);
        return mapToResponse(saved);
    }

    public List<UserProfileResponse> getAllProfiles() {
        return userProfileRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList(); // Modern Java 16+ replacement for .collect(Collectors.toList())
    }

    /**
     * Helper method to map Entity to Response DTO.
     */
    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .authUserId(profile.getAuthUserId())
                .fullName(profile.getFullName())
                .bio(profile.getBio())
                .profileImageUrl(profile.getProfileImageUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .githubUrl(profile.getGithubUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}