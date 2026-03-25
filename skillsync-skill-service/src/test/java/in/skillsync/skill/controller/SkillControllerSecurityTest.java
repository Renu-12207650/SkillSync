package in.skillsync.skill.controller;

import in.skillsync.common.security.JwtTokenProvider;
import in.skillsync.common.security.ServiceJwtFilter;
import in.skillsync.skill.dto.SkillResponse;
import in.skillsync.skill.security.SecurityConfig;
import in.skillsync.skill.service.SkillService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for SkillController.
 * Key focus: GET endpoints are open (no auth needed).
 * POST, PUT, DELETE are ROLE_ADMIN only.
 */
@WebMvcTest(SkillController.class)
@Import({SecurityConfig.class, ServiceJwtFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-unit-tests-must-be-long-enough-here",
        "skillsync.service-jwt-filter.enabled=true"
})
@DisplayName("SkillController Security Tests")
class SkillControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean  SkillService skillService;

    private static final String ADMIN_ID    = "4";
    private static final String LEARNER_ID  = "6";
    private static final String ROLE_ADMIN  = "ROLE_ADMIN";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";
    private static final String ROLE_MENTOR  = "ROLE_MENTOR";

    private static final String SKILL_JSON = """
            {
              "name": "Spring Boot",
              "description": "Enterprise Java framework",
              "category": "BACKEND"
            }
            """;

    private SkillResponse stubSkill() {
        return SkillResponse.builder()
                .id(1L).name("Spring Boot")
                .category("BACKEND").description("Enterprise Java framework")
                .build();
    }

    // ── GET endpoints — open to all (no token needed) ─────────────────────────

    @Test
    @DisplayName("GET /skills - no auth - returns 200 (open endpoint)")
    void getAllSkills_noAuth_returns200() throws Exception {
        when(skillService.getAllSkills()).thenReturn(List.of(stubSkill()));

        mockMvc.perform(get("/skills"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /skills/{id} - no auth - returns 200 (open endpoint)")
    void getSkillById_noAuth_returns200() throws Exception {
        when(skillService.getSkillById(1L)).thenReturn(stubSkill());

        mockMvc.perform(get("/skills/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /skills/category/{cat} - no auth - returns 200 (open endpoint)")
    void getSkillsByCategory_noAuth_returns200() throws Exception {
        when(skillService.getSkillsByCategory("BACKEND")).thenReturn(List.of(stubSkill()));

        mockMvc.perform(get("/skills/category/BACKEND"))
                .andExpect(status().isOk());
    }

    // ── POST — admin only ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /skills - no auth - returns 403")
    void createSkill_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/skills")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /skills - ROLE_LEARNER - returns 403")
    void createSkill_learnerRole_returns403() throws Exception {
        mockMvc.perform(post("/skills")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /skills - ROLE_MENTOR - returns 403")
    void createSkill_mentorRole_returns403() throws Exception {
        mockMvc.perform(post("/skills")
                .header("X-User-Id",   "7")
                .header("X-User-Role", ROLE_MENTOR)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /skills - ROLE_ADMIN - returns 201")
    void createSkill_adminRole_returns201() throws Exception {
        when(skillService.createSkill(any())).thenReturn(stubSkill());

        mockMvc.perform(post("/skills")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isCreated());
    }

    // ── PUT — admin only ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /skills/{id} - no auth - returns 403")
    void updateSkill_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/skills/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /skills/{id} - ROLE_LEARNER - returns 403")
    void updateSkill_learnerRole_returns403() throws Exception {
        mockMvc.perform(put("/skills/1")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /skills/{id} - ROLE_ADMIN - returns 200")
    void updateSkill_adminRole_returns200() throws Exception {
        when(skillService.updateSkill(anyLong(), any())).thenReturn(stubSkill());

        mockMvc.perform(put("/skills/1")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SKILL_JSON))
                .andExpect(status().isOk());
    }

    // ── DELETE — admin only ───────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /skills/{id} - no auth - returns 403")
    void deleteSkill_noAuth_returns403() throws Exception {
        mockMvc.perform(delete("/skills/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /skills/{id} - ROLE_LEARNER - returns 403")
    void deleteSkill_learnerRole_returns403() throws Exception {
        mockMvc.perform(delete("/skills/1")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /skills/{id} - ROLE_ADMIN - returns 204")
    void deleteSkill_adminRole_returns204() throws Exception {
        mockMvc.perform(delete("/skills/1")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN))
                .andExpect(status().isNoContent());
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /skills - ROLE_ADMIN - empty body - returns 400")
    void createSkill_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/skills")
                .header("X-User-Id",   ADMIN_ID)
                .header("X-User-Role", ROLE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}