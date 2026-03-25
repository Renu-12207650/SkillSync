package in.skillsync.auth.service;

import in.skillsync.auth.dto.AuthResponse;
import in.skillsync.auth.dto.LoginRequest;
import in.skillsync.auth.dto.RegisterRequest;
import in.skillsync.auth.entity.AuthUser;
import in.skillsync.auth.repository.AuthUserRepository;
import in.skillsync.common.exception.DuplicateEmailException;
import in.skillsync.common.exception.ResourceNotFoundException;
import in.skillsync.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for authentication operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user and returns JWT tokens.
     * Throws DuplicateEmailException if email already exists.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(
                    "Email is already registered: " + request.getEmail());
        }

        AuthUser user = AuthUser.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .build();

        AuthUser savedUser = authUserRepository.save(user);
        log.info("New user registered: {} with role: {}", savedUser.getEmail(), savedUser.getRole());

        return buildAuthResponse(savedUser);
    }

    /**
     * Authenticates user credentials and returns JWT tokens.
     * Spring Security validates credentials via AuthenticationManager.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + request.getEmail()));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * Builds AuthResponse with access and refresh tokens.
     */
    private AuthResponse buildAuthResponse(AuthUser user) {
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }
}
