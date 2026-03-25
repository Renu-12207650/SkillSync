package in.skillsync.session.controller;

import in.skillsync.common.security.JwtTokenProvider;
import in.skillsync.common.security.ServiceJwtFilter;
import in.skillsync.session.security.SecurityConfig;
import in.skillsync.session.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import({SecurityConfig.class, ServiceJwtFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-unit-tests-must-be-long-enough-here",
        "skillsync.service-jwt-filter.enabled=true"
})
@DisplayName("SessionController Security Tests")
class SessionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private static final String LEARNER_ID = "20";
    private static final String MENTOR_ID = "10";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";
    private static final String ROLE_MENTOR = "ROLE_MENTOR";

    // ── No Auth Tests (Expect 403 Forbidden) ─────────────────────────────────

    @Test
    void bookSession_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mentorId\": 10, \"skillId\": 1, \"sessionDateTime\": \"2026-04-01T10:00:00\", \"durationMinutes\": 60}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptSession_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/sessions/1/accept"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectSession_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/sessions/1/reject"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelSession_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/sessions/1/cancel"))
                .andExpect(status().isForbidden());
    }

    @Test
    void completeSession_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/sessions/1/complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMySessionsAsLearner_noAuth_returns403() throws Exception {
        // FIXED: Using actual URL from controller
        mockMvc.perform(get("/sessions/my/as-learner"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMySessionsAsMentor_noAuth_returns403() throws Exception {
        // FIXED: Using actual URL from controller
        mockMvc.perform(get("/sessions/my/as-mentor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSessionById_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/sessions/1"))
                .andExpect(status().isForbidden());
    }

    // ── Validation Tests ─────────────────────────────────────────────────────

    @Test
    void bookSession_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/sessions")
                        .header("X-User-Id", LEARNER_ID)
                        .header("X-User-Role", ROLE_LEARNER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── Role-Based Tests ─────────────────────────────────────────────────────

    @Test
    void bookSession_learnerRole_returns201() throws Exception {
        mockMvc.perform(post("/sessions")
                        .header("X-User-Id", LEARNER_ID)
                        .header("X-User-Role", ROLE_LEARNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mentorId\": 10, \"skillId\": 1, \"sessionDateTime\": \"2026-04-01T10:00:00\", \"durationMinutes\": 60}"))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptSession_mentorRole_returns200() throws Exception {
        mockMvc.perform(put("/sessions/1/accept")
                        .header("X-User-Id", MENTOR_ID)
                        .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }

    @Test
    void rejectSession_mentorRole_returns200() throws Exception {
        mockMvc.perform(put("/sessions/1/reject")
                        .header("X-User-Id", MENTOR_ID)
                        .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }

    @Test
    void cancelSession_learnerRole_returns200() throws Exception {
        mockMvc.perform(put("/sessions/1/cancel")
                        .header("X-User-Id", LEARNER_ID)
                        .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    void completeSession_mentorRole_returns200() throws Exception {
        mockMvc.perform(put("/sessions/1/complete")
                        .header("X-User-Id", MENTOR_ID)
                        .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }

    @Test
    void getMySessionsAsLearner_learnerRole_returns200() throws Exception {
        // FIXED: Using actual URL from controller
        mockMvc.perform(get("/sessions/my/as-learner")
                        .header("X-User-Id", LEARNER_ID)
                        .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    void getMySessionsAsMentor_mentorRole_returns200() throws Exception {
        // FIXED: Using actual URL from controller
        mockMvc.perform(get("/sessions/my/as-mentor")
                        .header("X-User-Id", MENTOR_ID)
                        .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }

    @Test
    void getSessionById_learnerRole_returns200() throws Exception {
        mockMvc.perform(get("/sessions/1")
                        .header("X-User-Id", LEARNER_ID)
                        .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    void getSessionById_mentorRole_returns200() throws Exception {
        mockMvc.perform(get("/sessions/1")
                        .header("X-User-Id", MENTOR_ID)
                        .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }
}