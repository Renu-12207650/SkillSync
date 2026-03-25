package in.skillsync.notification.controller;

import in.skillsync.common.security.JwtTokenProvider;
import in.skillsync.common.security.ServiceJwtFilter;
import in.skillsync.notification.config.SecurityConfig;
import in.skillsync.notification.dto.NotificationResponse;
import in.skillsync.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security tests for NotificationController.
 * All notification endpoints require authentication.
 * Any authenticated user can only see their own notifications (via X-User-Id).
 */
@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, ServiceJwtFilter.class, JwtTokenProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-for-unit-tests-must-be-long-enough-here",
        "skillsync.service-jwt-filter.enabled=true",
        "spring.rabbitmq.host=localhost",
        "spring.mail.host=smtp.gmail.com",
        "spring.mail.username=test@test.com",
        "spring.mail.password=test"
})
@DisplayName("NotificationController Security Tests")
class NotificationControllerSecurityTest {
    
    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired MockMvc mockMvc;
    
    @MockBean  
    private NotificationService notificationService;

    private static final String LEARNER_ID   = "6";
    private static final String MENTOR_ID    = "7";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";
    private static final String ROLE_MENTOR  = "ROLE_MENTOR";

    private NotificationResponse stubNotification() {
        return NotificationResponse.builder()
                .id(1L)
                .recipientUserId(Long.valueOf(LEARNER_ID))
                .type("SESSION_BOOKED")
                .title("Session booked")
                .message("Your session has been booked")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── No auth — expect 403 on all endpoints ─────────────────────────────────

    @Test
    @DisplayName("GET /notifications/my - no auth - returns 403")
    void getMyNotifications_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/notifications/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /notifications/my/unread - no auth - returns 403")
    void getUnreadNotifications_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/notifications/my/unread"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /notifications/my/unread-count - no auth - returns 403")
    void getUnreadCount_noAuth_returns403() throws Exception {
        mockMvc.perform(get("/notifications/my/unread-count"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /notifications/{id}/read - no auth - returns 403")
    void markAsRead_noAuth_returns403() throws Exception {
        mockMvc.perform(put("/notifications/1/read"))
                .andExpect(status().isForbidden());
    }

    // ── ROLE_LEARNER — all endpoints accessible ───────────────────────────────

    @Test
    @DisplayName("GET /notifications/my - ROLE_LEARNER - returns 200")
    void getMyNotifications_learnerRole_returns200() throws Exception {
        when(notificationService.getAllNotifications(anyLong()))
                .thenReturn(List.of(stubNotification()));

        mockMvc.perform(get("/notifications/my")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/my/unread - ROLE_LEARNER - returns 200")
    void getUnreadNotifications_learnerRole_returns200() throws Exception {
        when(notificationService.getUnreadNotifications(anyLong()))
                .thenReturn(List.of(stubNotification()));

        mockMvc.perform(get("/notifications/my/unread")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/my/unread-count - ROLE_LEARNER - returns 200")
    void getUnreadCount_learnerRole_returns200() throws Exception {
        when(notificationService.getUnreadCount(anyLong())).thenReturn(3L);

        mockMvc.perform(get("/notifications/my/unread-count")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /notifications/{id}/read - ROLE_LEARNER - returns 200")
    void markAsRead_learnerRole_returns200() throws Exception {
        when(notificationService.markAsRead(anyLong()))
                .thenReturn(stubNotification());

        mockMvc.perform(put("/notifications/1/read")
                .header("X-User-Id",   LEARNER_ID)
                .header("X-User-Role", ROLE_LEARNER))
                .andExpect(status().isOk());
    }

    // ── ROLE_MENTOR — all endpoints accessible ────────────────────────────────

    @Test
    @DisplayName("GET /notifications/my - ROLE_MENTOR - returns 200")
    void getMyNotifications_mentorRole_returns200() throws Exception {
        when(notificationService.getAllNotifications(anyLong()))
                .thenReturn(List.of());

        mockMvc.perform(get("/notifications/my")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /notifications/my/unread-count - ROLE_MENTOR - returns 200")
    void getUnreadCount_mentorRole_returns200() throws Exception {
        when(notificationService.getUnreadCount(anyLong())).thenReturn(1L);

        mockMvc.perform(get("/notifications/my/unread-count")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /notifications/{id}/read - ROLE_MENTOR - returns 200")
    void markAsRead_mentorRole_returns200() throws Exception {
        when(notificationService.markAsRead(anyLong()))
                .thenReturn(stubNotification());

        mockMvc.perform(put("/notifications/1/read")
                .header("X-User-Id",   MENTOR_ID)
                .header("X-User-Role", ROLE_MENTOR))
                .andExpect(status().isOk());
    }
}