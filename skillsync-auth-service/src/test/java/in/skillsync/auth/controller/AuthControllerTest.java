package in.skillsync.auth.controller;

import in.skillsync.auth.dto.AuthResponse;
import in.skillsync.auth.dto.LoginRequest;
import in.skillsync.auth.dto.RegisterRequest;
import in.skillsync.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Unit Tests")
class AuthControllerTest {

        @Mock
        private AuthService authService;

        @InjectMocks
        private AuthController authController;

        @Test
        @DisplayName("Register - Returns 201 Created")
        void register_ReturnsCreated() {
                RegisterRequest request = new RegisterRequest();
                AuthResponse mockResponse = AuthResponse.builder().accessToken("token").build();

                when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

                ResponseEntity<AuthResponse> response = authController.register(request);

                assertEquals(HttpStatus.CREATED, response.getStatusCode());
                assertEquals(mockResponse, response.getBody());
                verify(authService).register(request);
        }

        @Test
        @DisplayName("Login - Returns 200 OK")
        void login_ReturnsOk() {
                LoginRequest request = new LoginRequest();
                AuthResponse mockResponse = AuthResponse.builder().accessToken("token").build();

                when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

                ResponseEntity<AuthResponse> response = authController.login(request);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(mockResponse, response.getBody());
                verify(authService).login(request);
        }

    @Test
    @DisplayName("GetEmailById - Returns 200 OK")
    void getEmailById_ReturnsOk() {
        when(authService.getUserEmailById(1L)).thenReturn("user@skillsync.in");

        ResponseEntity<String> response = authController.getEmailById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("user@skillsync.in", response.getBody());
        verify(authService).getUserEmailById(1L);
    }
}