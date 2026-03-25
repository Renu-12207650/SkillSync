package in.skillsync.notification.service;

import in.skillsync.notification.dto.SessionEventPayload;
import in.skillsync.notification.entity.Notification;
import in.skillsync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;

    @InjectMocks private NotificationService notificationService;

    private SessionEventPayload payload;

    @BeforeEach
    void setUp() {
        payload = SessionEventPayload.builder()
                .sessionId(1L)
                .mentorId(10L)
                .learnerId(20L)
                .sessionDateTime(LocalDateTime.now().plusDays(2))
                .topic("Spring Boot Basics")
                .eventType("SESSION_BOOKED")
                .build();
    }

    @Test
    @DisplayName("handleSessionBooked - saves two notifications (mentor + learner)")
    void handleSessionBooked_savesTwoNotifications() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.handleSessionBooked(payload);

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("handleSessionAccepted - saves notification for learner")
    void handleSessionAccepted_savesLearnerNotification() {
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.handleSessionAccepted(payload);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUserId()).isEqualTo(20L);
        assertThat(saved.getType()).isEqualTo("SESSION_ACCEPTED");
        assertThat(saved.isRead()).isFalse();
    }

    @Test
    @DisplayName("getUnreadCount - returns correct count")
    void getUnreadCount_returnsCorrectCount() {
        when(notificationRepository.countByRecipientUserIdAndReadFalse(20L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(20L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("markAsRead - sets read flag to true")
    void markAsRead_setsReadFlagTrue() {
        Notification notification = Notification.builder()
                .id(1L).recipientUserId(20L)
                .type("SESSION_BOOKED").title("Test").message("Test msg")
                .read(false).build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = notificationService.markAsRead(1L);

        assertThat(response.isRead()).isTrue();
    }
}
