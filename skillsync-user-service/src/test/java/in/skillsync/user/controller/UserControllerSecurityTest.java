package in.skillsync.user.controller;

import in.skillsync.common.security.JwtTokenProvider;
import in.skillsync.common.security.ServiceJwtFilter;
import in.skillsync.user.dto.UserProfileResponse;
import in.skillsync.user.security.SecurityConfig;
import in.skillsync.user.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for UserProfileController.
 * Tests 403 (no auth), 403 (wrong role), 200/201 (correct role).
 */
@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, ServiceJwtFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-unit-tests-must-be-long-enough-here",
        "skillsync.service-jwt-filter.enabled=true"
})
@DisplayName("UserController Security Tests")
class UserControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean  UserProfileService userProfileService;

    private static final String LEARNER_ID   = "6";
    private static final String ADMIN_ID     = "4";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";
    private static final String ROLE_ADMIN   = "ROLE_ADMIN";

    private static final String VALID_PROFILE_JSON = """
            {
              "fullName": "Renu Dhankhar",
              "bio": "Java developer",
              "profileImageUrl": "",
              "linkedinUrl": "",
              "githubUrl": ""
            }
            """;

    // ── No auth — expect 403 ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /users - no auth headers - returns 403")
    void createProfile_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/me - no auth headers - returns 403")
    void getMyProfile_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /users/me - no auth headers - returns 403")
    void updateMyProfile_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users/{authUserId} - no auth headers - returns 403")
    void getProfileById_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users - no auth headers - returns 403")
    void getAllProfiles_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    // ── ROLE_LEARNER — correct access ─────────────────────────────────────────

    @Test
    @DisplayName("POST /users - ROLE_LEARNER headers - returns 201")
    void createProfile_learnerRole_returns201() throws Exception {
        when(userProfileService.createProfile(anyLong(), any()))
                .thenReturn(UserProfileResponse.builder()
                        .id(1L).authUserId(Long.valueOf(LEARNER_ID))
                        .fullName("Renu Dhankhar").build());

        mockMvc.perform(post("/users")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /users/me - ROLE_LEARNER headers - returns 200")
    void getMyProfile_learnerRole_returns200() throws Exception {
        when(userProfileService.getProfileByAuthUserId(anyLong()))
                .thenReturn(UserProfileResponse.builder()
                        .id(1L).authUserId(Long.valueOf(LEARNER_ID))
                        .fullName("Renu Dhankhar").build());

        mockMvc.perform(get("/users/me")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /users/me - ROLE_LEARNER headers - returns 200")
    void updateMyProfile_learnerRole_returns200() throws Exception {
        when(userProfileService.updateProfile(anyLong(), any()))
                .thenReturn(UserProfileResponse.builder()
                        .id(1L).authUserId(Long.valueOf(LEARNER_ID))
                        .fullName("Renu Dhankhar").build());

        mockMvc.perform(put("/users/me")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_PROFILE_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users/{id} - ROLE_LEARNER headers - returns 200")
    void getProfileById_learnerRole_returns200() throws Exception {
        when(userProfileService.getProfileByAuthUserId(anyLong()))
                .thenReturn(UserProfileResponse.builder()
                        .id(1L).authUserId(1L)
                        .fullName("Someone").build());

        mockMvc.perform(get("/users/1")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    // ── GET /users — admin only ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /users - ROLE_LEARNER - returns 403 (admin only endpoint)")
    void getAllProfiles_learnerRole_returns403() throws Exception {
        mockMvc.perform(get("/users")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users - ROLE_MENTOR - returns 403 (admin only endpoint)")
    void getAllProfiles_mentorRole_returns403() throws Exception {
        mockMvc.perform(get("/users")
                .header("X-User-Id",   "7")
                .header("X-User-Role", "ROLE_MENTOR"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /users - ROLE_ADMIN - returns 200")
    void getAllProfiles_adminRole_returns200() throws Exception {
        when(userProfileService.getAllProfiles()).thenReturn(List.of());

        mockMvc.perform(get("/users")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN))
                .andExpect(status().isOk());
    }
}