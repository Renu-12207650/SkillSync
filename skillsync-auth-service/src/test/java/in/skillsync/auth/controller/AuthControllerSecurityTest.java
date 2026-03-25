package in.skillsync.auth.controller;

import in.skillsync.auth.repository.AuthUserRepository;
import in.skillsync.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for AuthController.
 * Verifies that all auth endpoints are publicly accessible (no JWT required),
 * and that input validation is enforced.
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController Security Tests")
class AuthControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthService authService;

    // Required by SecurityConfig → CustomUserDetailsService → AuthUserRepository
    @MockBean AuthUserRepository authUserRepository;

    // ── Open endpoint tests (no token needed) ─────────────────────────────────

    @Test
    @DisplayName("POST /auth/register - no token - should be accessible (permit all)")
    void register_withoutToken_isAccessible() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Renu Dhankhar",
                      "email": "renu@skillsync.com",
                      "password": "Password123!",
                      "role": "ROLE_LEARNER"
                    }
                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /auth/login - no token - should be accessible (permit all)")
    void login_withoutToken_isAccessible() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "renu@skillsync.com",
                      "password": "Password123!"
                    }
                """))
                .andExpect(status().isOk());
    }

    // ── Input validation tests ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register - missing fullName - returns 400")
    void register_missingFullName_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "renu@skillsync.com",
                      "password": "Password123!",
                      "role": "ROLE_LEARNER"
                    }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - missing email - returns 400")
    void register_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Renu Dhankhar",
                      "password": "Password123!",
                      "role": "ROLE_LEARNER"
                    }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - invalid email format - returns 400")
    void register_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Renu Dhankhar",
                      "email": "not-an-email",
                      "password": "Password123!",
                      "role": "ROLE_LEARNER"
                    }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - missing password - returns 400")
    void register_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Renu Dhankhar",
                      "email": "renu@skillsync.com",
                      "role": "ROLE_LEARNER"
                    }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register - empty body - returns 400")
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - missing email - returns 400")
    void login_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "password": "Password123!"
                    }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - missing password - returns 400")
    void login_missingPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "renu@skillsync.com"
                    }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/login - malformed JSON body - returns 400")
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not-json"))
                .andExpect(status().isBadRequest());
    }
}
