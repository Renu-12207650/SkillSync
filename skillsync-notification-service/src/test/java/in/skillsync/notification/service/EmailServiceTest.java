package in.skillsync.notification.service;

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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email Service Unit Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendEmail - Success")
    void sendEmail_Success() {
        // Act
        emailService.sendEmail("test@skillsync.in", "Subject", "Body");

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("test@skillsync.in", sentMessage.getTo()[0]);
        assertEquals("Subject", sentMessage.getSubject());
        assertEquals("Body", sentMessage.getText());
        assertEquals("renudhankhar8559@gmail.com", sentMessage.getFrom());
    }

    @Test
    @DisplayName("sendEmail - Exception is safely caught")
    void sendEmail_ThrowsException_IsCaught() {
        // Arrange
        doThrow(new MailSendException("SMTP Server failed"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert (Should not throw an exception upwards)
        assertDoesNotThrow(() -> {
            emailService.sendEmail("test@skillsync.in", "Subject", "Body");
        });

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}