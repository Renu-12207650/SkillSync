package in.skillsync.mentor.controller;

import in.skillsync.common.security.JwtTokenProvider;
import in.skillsync.common.security.ServiceJwtFilter;
import in.skillsync.mentor.dto.MentorProfileResponse;
import in.skillsync.mentor.entity.MentorStatus;
import in.skillsync.mentor.security.SecurityConfig;
import in.skillsync.mentor.service.MentorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for MentorController.
 * Focuses on admin-only endpoints (pending, approve, reject)
 * and role-based access control.
 */
@WebMvcTest(MentorController.class)
@Import({SecurityConfig.class, ServiceJwtFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-unit-tests-must-be-long-enough-here",
        "skillsync.service-jwt-filter.enabled=true"
})
@DisplayName("MentorController Security Tests")
class MentorControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean  MentorService mentorService;

    private static final String LEARNER_ID   = "6";
    private static final String MENTOR_ID    = "7";
    private static final String ADMIN_ID     = "4";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";
    private static final String ROLE_MENTOR  = "ROLE_MENTOR";
    private static final String ROLE_ADMIN   = "ROLE_ADMIN";

    private static final String APPLY_JSON = """
            {
              "bio": "Experienced Java developer",
              "yearsOfExperience": 5,
              "hourlyRate": 500.00,
              "skillIds": [1, 2]
            }
            """;

    private MentorProfileResponse stubMentor(MentorStatus status) {
        return MentorProfileResponse.builder()
                .id(1L).authUserId(Long.valueOf(MENTOR_ID))
                .bio("bio").yearsOfExperience(5)
                .hourlyRate(new BigDecimal("500"))
                .status(status).build();
    }

    // ── No auth — expect 401 ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /mentors/pending - no auth - returns 401")
    void getPendingMentors_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/mentors/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/approve - no auth - returns 401")
    void approveMentor_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/mentors/1/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/reject - no auth - returns 401")
    void rejectMentor_noAuth_returns401() throws Exception {
        mockMvc.perform(put("/mentors/1/reject"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /mentors/apply - no auth - returns 401")
    void applyAsMentor_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/mentors/apply")
                .contentType(MediaType.APPLICATION_JSON)
                .content(APPLY_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /mentors/my-profile - no auth - returns 401")
    void getMyMentorProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/mentors/my-profile"))
                .andExpect(status().isUnauthorized());
    }

    // ── ROLE_LEARNER on admin-only endpoints — expect 403 ────────────────────

    @Test
    @DisplayName("GET /mentors/pending - ROLE_LEARNER - returns 403")
    void getPendingMentors_learnerRole_returns403() throws Exception {
        mockMvc.perform(get("/mentors/pending")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/approve - ROLE_LEARNER - returns 403")
    void approveMentor_learnerRole_returns403() throws Exception {
        mockMvc.perform(put("/mentors/1/approve")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/reject - ROLE_LEARNER - returns 403")
    void rejectMentor_learnerRole_returns403() throws Exception {
        mockMvc.perform(put("/mentors/1/reject")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isForbidden());
    }

    // ── ROLE_MENTOR on admin-only endpoints — expect 403 ─────────────────────

    @Test
    @DisplayName("GET /mentors/pending - ROLE_MENTOR - returns 403")
    void getPendingMentors_mentorRole_returns403() throws Exception {
        mockMvc.perform(get("/mentors/pending")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/approve - ROLE_MENTOR - returns 403")
    void approveMentor_mentorRole_returns403() throws Exception {
        mockMvc.perform(put("/mentors/1/approve")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isForbidden());
    }

    // ── ROLE_ADMIN on admin-only endpoints — expect 200 ──────────────────────

    @Test
    @DisplayName("GET /mentors/pending - ROLE_ADMIN - returns 200")
    void getPendingMentors_adminRole_returns200() throws Exception {
        when(mentorService.getPendingMentors(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(stubMentor(MentorStatus.PENDING_APPROVAL))));

        mockMvc.perform(get("/mentors/pending")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/approve - ROLE_ADMIN - returns 200")
    void approveMentor_adminRole_returns200() throws Exception {
        when(mentorService.approveMentor(1L)).thenReturn(stubMentor(MentorStatus.ACTIVE));

        mockMvc.perform(put("/mentors/1/approve")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /mentors/{id}/reject - ROLE_ADMIN - returns 200")
    void rejectMentor_adminRole_returns200() throws Exception {
        when(mentorService.rejectMentor(1L)).thenReturn(stubMentor(MentorStatus.REJECTED));

        mockMvc.perform(put("/mentors/1/reject")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN))
                .andExpect(status().isOk());
    }

    // ── ROLE_MENTOR on mentor-specific endpoints — expect 200 ────────────────

    @Test
    @DisplayName("POST /mentors/apply - ROLE_MENTOR - returns 201")
    void applyAsMentor_mentorRole_returns201() throws Exception {
        when(mentorService.applyAsMentor(anyLong(), any()))
                .thenReturn(stubMentor(MentorStatus.PENDING_APPROVAL));

        mockMvc.perform(post("/mentors/apply")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(APPLY_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /mentors/my-profile - ROLE_MENTOR - returns 200")
    void getMyMentorProfile_mentorRole_returns200() throws Exception {
        when(mentorService.getMentorByAuthUserId(anyLong()))
                .thenReturn(stubMentor(MentorStatus.ACTIVE));

        mockMvc.perform(get("/mentors/my-profile")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }
}
