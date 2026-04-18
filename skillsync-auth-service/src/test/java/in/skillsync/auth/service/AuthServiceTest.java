package in.skillsync.auth.service;

import in.skillsync.auth.dto.AuthResponse;
import in.skillsync.auth.dto.LoginRequest;
import in.skillsync.auth.dto.RegisterRequest;
import in.skillsync.auth.entity.AuthUser;
import in.skillsync.auth.entity.Role;
import in.skillsync.auth.repository.AuthUserRepository;
import in.skillsync.common.exception.DuplicateEmailException;
import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    // ==========================================
    // Tests for getUserEmailById()
    // ==========================================

    @Test
    @DisplayName("getUserEmailById - Success returns email")
    void getUserEmailById_Success_ReturnsEmail() {
        // Arrange
        AuthUser mockUser = AuthUser.builder().email("test@skillsync.in").build();
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // Act
        String email = authService.getUserEmailById(1L);

        // Assert
        assertEquals("test@skillsync.in", email);
        verify(authUserRepository).findById(1L);
    }

    @Test
    @DisplayName("getUserEmailById - Not Found throws Exception")
    void getUserEmailById_NotFound_ThrowsException() {
        // Arrange
        when(authUserRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.getUserEmailById(99L);
        });
    }

    // ==========================================
    // Tests for register()
    // ==========================================

    @Test
    @DisplayName("register - Duplicate Email throws Exception")
    void register_DuplicateEmail_ThrowsException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@skillsync.in");

        when(authUserRepository.existsByEmail("duplicate@skillsync.in")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEmailException.class, () -> {
            authService.register(request);
        });

        // Verify save was never called
        verify(authUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("register - Success returns AuthResponse")
    void register_Success_ReturnsAuthResponse() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("newuser@skillsync.in");
        request.setPassword("password123");
        request.setRole(Role.ROLE_LEARNER); // If your enum is named differently, update this!

        when(authUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // Mock the saved user with an ID
        AuthUser savedUser = AuthUser.builder()
                .id(100L)
                .fullName("John Doe")
                .email("newuser@skillsync.in")
                .role(Role.ROLE_LEARNER)
                .build();

        when(authUserRepository.save(any(AuthUser.class))).thenReturn(savedUser);

        // Mock token generation
        when(jwtTokenProvider.generateAccessToken(100L, "newuser@skillsync.in", "ROLE_LEARNER"))
                .thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(100L))
                .thenReturn("mock-refresh-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("newuser@skillsync.in", response.getEmail());
        verify(authUserRepository).save(any(AuthUser.class));
    }

    // ==========================================
    // Tests for login()
    // ==========================================

    @Test
    @DisplayName("login - User Not Found throws Exception")
    void login_UserNotFound_ThrowsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@skillsync.in");
        request.setPassword("password123");

        // Assume AuthenticationManager passes, but DB lookup fails
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(authUserRepository.findByEmail("missing@skillsync.in")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.login(request);
        });
    }

    @Test
    @DisplayName("login - Success returns AuthResponse")
    void login_Success_ReturnsAuthResponse() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("exist@skillsync.in");
        request.setPassword("password123");

        AuthUser mockUser = AuthUser.builder()
                .id(200L)
                .fullName("Jane Doe")
                .email("exist@skillsync.in")
                .role(Role.ROLE_MENTOR)
                .build();

        // Mock Auth Manager (Just needs to not throw an exception)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        // Mock DB lookup
        when(authUserRepository.findByEmail("exist@skillsync.in")).thenReturn(Optional.of(mockUser));

        // Mock token generation
        when(jwtTokenProvider.generateAccessToken(200L, "exist@skillsync.in", "ROLE_MENTOR"))
                .thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(200L))
                .thenReturn("mock-refresh-token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals(200L, response.getUserId());
        assertEquals("ROLE_MENTOR", response.getRole());
    }
}