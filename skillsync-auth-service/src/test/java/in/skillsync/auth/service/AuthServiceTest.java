package in.skillsync.auth.service;

import in.skillsync.auth.dto.LoginRequest;
import in.skillsync.auth.dto.RegisterRequest;
import in.skillsync.auth.entity.AuthUser;
import in.skillsync.auth.entity.Role;
import in.skillsync.auth.repository.AuthUserRepository;
import in.skillsync.common.exception.DuplicateEmailException;
import in.skillsync.common.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private AuthUserRepository authUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private AuthUser savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Renu Dhankhar");
        registerRequest.setEmail("renu@skillsync.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setRole(Role.ROLE_LEARNER);

        savedUser = AuthUser.builder()
                .id(1L)
                .fullName("Renu Dhankhar")
                .email("renu@skillsync.com")
                .password("encodedPassword")
                .role(Role.ROLE_LEARNER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("register - success - returns AuthResponse with tokens")
    void register_success_returnsAuthResponseWithTokens() {
        when(authUserRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(authUserRepository.save(any())).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        var response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getEmail()).isEqualTo("renu@skillsync.com");
        assertThat(response.getRole()).isEqualTo("ROLE_LEARNER");
        verify(authUserRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("register - duplicate email - throws DuplicateEmailException")
    void register_duplicateEmail_throwsDuplicateEmailException() {
        when(authUserRepository.existsByEmail("renu@skillsync.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email is already registered");

        verify(authUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - valid credentials - returns AuthResponse")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("renu@skillsync.com");
        loginRequest.setPassword("Password123!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(authUserRepository.findByEmail("renu@skillsync.com"))
                .thenReturn(Optional.of(savedUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        var response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }
}
