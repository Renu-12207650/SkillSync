package in.skillsync.notification.service;

import in.skillsync.notification.dto.NotificationResponse;
import in.skillsync.notification.dto.SessionEventPayload;
import in.skillsync.notification.entity.Notification;
import in.skillsync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    // ==========================================
    // 1. Event Handler Tests
    // ==========================================

    @Test
    @DisplayName("handleSessionBooked saves two notifications (Mentor & Learner)")
    void handleSessionBooked_SavesTwoNotifications() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getMentorId()).thenReturn(10L);
        when(payload.getLearnerId()).thenReturn(20L);
        when(payload.getTopic()).thenReturn("Java Spring Boot");

        notificationService.handleSessionBooked(payload);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("handleSessionAccepted saves one notification")
    void handleSessionAccepted_SavesOneNotification() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getLearnerId()).thenReturn(20L);
        when(payload.getTopic()).thenReturn("Java Spring Boot");

        notificationService.handleSessionAccepted(payload);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("handleSessionRejected saves one notification")
    void handleSessionRejected_SavesOneNotification() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getLearnerId()).thenReturn(20L);

        notificationService.handleSessionRejected(payload);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("handleSessionCompleted saves one notification")
    void handleSessionCompleted_SavesOneNotification() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getLearnerId()).thenReturn(20L);

        notificationService.handleSessionCompleted(payload);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    @DisplayName("handleMentorApproved saves one notification")
    void handleMentorApproved_SavesOneNotification() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getMentorId()).thenReturn(10L);

        notificationService.handleMentorApproved(payload);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    // ==========================================
    // 2. Query Methods Tests
    // ==========================================

    @Test
    @DisplayName("getAllNotifications returns mapped list")
    void getAllNotifications_ReturnsMappedList() {
        Notification notification = createMockNotification();
        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getAllNotifications(10L);

        assertEquals(1, result.size());
        assertEquals("Test Title", result.get(0).getTitle());
        verify(notificationRepository).findByRecipientUserIdOrderByCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("getUnreadNotifications returns mapped list")
    void getUnreadNotifications_ReturnsMappedList() {
        Notification notification = createMockNotification();
        when(notificationRepository.findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getUnreadNotifications(10L);

        assertEquals(1, result.size());
        verify(notificationRepository).findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(10L);
    }

    @Test
    @DisplayName("getUnreadCount returns correct count")
    void getUnreadCount_ReturnsCount() {
        when(notificationRepository.countByRecipientUserIdAndReadFalse(10L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(10L);

        assertEquals(5L, count);
        verify(notificationRepository).countByRecipientUserIdAndReadFalse(10L);
    }

    // ==========================================
    // 3. Mark As Read Tests
    // ==========================================

    @Test
    @DisplayName("markAsRead updates status and returns response")
    void markAsRead_Success_UpdatesStatus() {
        Notification notification = createMockNotification();
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(100L);

        assertTrue(response.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead throws Exception when not found")
    void markAsRead_NotFound_ThrowsException() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(99L));
        verify(notificationRepository, never()).save(any());
    }

    // ==========================================
    // 4. Private Email Helper Tests (Using Reflection)
    // ==========================================

    @Test
    @DisplayName("saveAndNotify with email triggers JavaMailSender")
    void saveAndNotify_WithEmail_SendsEmailSuccessfully() {
        // We use ReflectionTestUtils to invoke the private method
        ReflectionTestUtils.invokeMethod(notificationService, "saveAndNotify",
                1L, "TYPE", "Test Subject", "Test Body", "user@skillsync.in");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentEmail = messageCaptor.getValue();
        assertEquals("user@skillsync.in", sentEmail.getTo()[0]);
        assertEquals("[SkillSync] Test Subject", sentEmail.getSubject());
    }

    @Test
    @DisplayName("saveAndNotify email exception is safely caught and logged")
    void saveAndNotify_EmailException_IsCaughtSafely() {
        doThrow(new MailSendException("SMTP Server Down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // This should NOT throw an exception because the catch block in sendEmail
        // handles it
        ReflectionTestUtils.invokeMethod(notificationService, "saveAndNotify",
                1L, "TYPE", "Title", "Body", "bad@email.com");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private Notification createMockNotification() {
        return Notification.builder()
                .id(100L)
                .recipientUserId(10L)
                .type("SESSION_BOOKED")
                .title("Test Title")
                .message("Test Message")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}