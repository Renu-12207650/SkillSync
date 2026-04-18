package in.skillsync.notification.messaging;

import in.skillsync.notification.client.AuthClient;
import in.skillsync.notification.dto.SessionEventPayload;
import in.skillsync.notification.service.EmailService;
import in.skillsync.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Event Consumer Unit Tests")
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthClient authClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    @DisplayName("Null Event Type - Skips processing")
    void handleEvent_NullEventType_ReturnsEarly() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn(null);

        consumer.handleEvent(payload);

        verifyNoInteractions(notificationService, authClient, emailService);
    }

    @Test
    @DisplayName("SESSION_BOOKED - Triggers DB and sends Email")
    void handleEvent_SessionBooked_ProcessesAndSendsEmail() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn("SESSION_BOOKED");
        when(payload.getLearnerId()).thenReturn(100L);

        when(authClient.getUserEmail(100L)).thenReturn("learner@skillsync.in");

        consumer.handleEvent(payload);

        verify(notificationService).handleSessionBooked(payload);
        verify(emailService).sendEmail(eq("learner@skillsync.in"), anyString(), anyString());
    }

    @Test
    @DisplayName("SESSION_ACCEPTED - Handles null email")
    void handleEvent_SessionAccepted_NullEmail_SkipsEmail() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn("SESSION_ACCEPTED");
        when(payload.getLearnerId()).thenReturn(200L);

        when(authClient.getUserEmail(200L)).thenReturn(null);

        consumer.handleEvent(payload);

        verify(notificationService).handleSessionAccepted(payload);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("SESSION_REJECTED - Catch Feign Exception")
    void handleEvent_SessionRejected_FeignException_Caught() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn("SESSION_REJECTED");
        when(payload.getLearnerId()).thenReturn(300L);

        when(authClient.getUserEmail(300L)).thenThrow(new RuntimeException("Auth Down"));

        consumer.handleEvent(payload);

        verify(notificationService).handleSessionRejected(payload);
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("SESSION_COMPLETED - DB Only")
    void handleEvent_SessionCompleted_DbOnly() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn("SESSION_COMPLETED");

        consumer.handleEvent(payload);

        verify(notificationService).handleSessionCompleted(payload);
        verifyNoInteractions(authClient, emailService);
    }

    @Test
    @DisplayName("MENTOR_APPROVED - DB Only")
    void handleEvent_MentorApproved_DbOnly() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn("MENTOR_APPROVED");

        consumer.handleEvent(payload);

        verify(notificationService).handleMentorApproved(payload);
        verifyNoInteractions(authClient, emailService);
    }

    @Test
    @DisplayName("UNKNOWN_EVENT - Default Case")
    void handleEvent_UnknownEvent_HitsDefault() {
        SessionEventPayload payload = mock(SessionEventPayload.class);
        when(payload.getEventType()).thenReturn("RANDOM");

        consumer.handleEvent(payload);

        verifyNoInteractions(notificationService, authClient, emailService);
    }
}