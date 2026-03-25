package in.skillsync.session.service;

import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.exception.UnauthorizedActionException;
import in.skillsync.session.dto.RejectSessionRequest;
import in.skillsync.session.dto.SessionRequest;
import in.skillsync.session.entity.MentoringSession;
import in.skillsync.session.entity.SessionStatus;
import in.skillsync.session.messaging.SessionEventPublisher;
import in.skillsync.session.repository.MentoringSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService Unit Tests")
class SessionServiceTest {

    @Mock private MentoringSessionRepository sessionRepository;
    @Mock private SessionEventPublisher eventPublisher;

    @InjectMocks private SessionService sessionService;

    private SessionRequest sessionRequest;
    private MentoringSession requestedSession;
    private MentoringSession acceptedSession;

    @BeforeEach
    void setUp() {
        sessionRequest = new SessionRequest();
        sessionRequest.setMentorId(10L);
        sessionRequest.setSessionDateTime(LocalDateTime.now().plusDays(3));
        sessionRequest.setDurationMinutes(60);
        sessionRequest.setTopic("Spring Boot Microservices");

        requestedSession = MentoringSession.builder()
                .id(1L)
                .mentorId(10L)
                .learnerId(20L)
                .sessionDateTime(LocalDateTime.now().plusDays(3))
                .durationMinutes(60)
                .topic("Spring Boot Microservices")
                .status(SessionStatus.REQUESTED)
                .build();

        acceptedSession = MentoringSession.builder()
                .id(2L)
                .mentorId(10L)
                .learnerId(20L)
                .sessionDateTime(LocalDateTime.now().plusDays(3))
                .durationMinutes(60)
                .topic("Spring Boot Microservices")
                .status(SessionStatus.ACCEPTED)
                .build();
    }

    @Test
    @DisplayName("bookSession - success - status is REQUESTED and event published")
    void bookSession_success_returnsRequestedStatus() {
        when(sessionRepository.save(any())).thenReturn(requestedSession);

        var response = sessionService.bookSession(20L, sessionRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SessionStatus.REQUESTED);
        assertThat(response.getMentorId()).isEqualTo(10L);
        verify(eventPublisher, times(1)).publishSessionBooked(any());
    }

    @Test
    @DisplayName("acceptSession - success - status changes to ACCEPTED")
    void acceptSession_success_statusBecomesAccepted() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(requestedSession));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = sessionService.acceptSession(1L, 10L);

        assertThat(response.getStatus()).isEqualTo(SessionStatus.ACCEPTED);
        verify(eventPublisher, times(1)).publishSessionAccepted(any());
    }

    @Test
    @DisplayName("acceptSession - wrong mentor - throws UnauthorizedActionException")
    void acceptSession_wrongMentor_throwsUnauthorizedException() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(requestedSession));

        assertThatThrownBy(() -> sessionService.acceptSession(1L, 99L))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessageContaining("not the mentor");

        verify(eventPublisher, never()).publishSessionAccepted(any());
    }

    @Test
    @DisplayName("rejectSession - with reason - status is REJECTED")
    void rejectSession_withReason_statusBecomesRejected() {
        RejectSessionRequest rejectRequest = new RejectSessionRequest();
        rejectRequest.setRejectionReason("Schedule conflict");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(requestedSession));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = sessionService.rejectSession(1L, 10L, rejectRequest);

        assertThat(response.getStatus()).isEqualTo(SessionStatus.REJECTED);
        assertThat(response.getRejectionReason()).isEqualTo("Schedule conflict");
        verify(eventPublisher, times(1)).publishSessionRejected(any());
    }

    @Test
    @DisplayName("completeSession - success - status changes to COMPLETED")
    void completeSession_success_statusBecomesCompleted() {
        when(sessionRepository.findById(2L)).thenReturn(Optional.of(acceptedSession));
        when(sessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = sessionService.completeSession(2L, 10L);

        assertThat(response.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        verify(eventPublisher, times(1)).publishSessionCompleted(any());
    }

    @Test
    @DisplayName("getSessionById - not found - throws ResourceNotFoundException")
    void getSessionById_notFound_throwsException() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getSessionById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
