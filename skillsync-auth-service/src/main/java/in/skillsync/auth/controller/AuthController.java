package in.skillsync.auth.controller;

import in.skillsync.auth.dto.AuthResponse;
import in.skillsync.auth.dto.LoginRequest;
import in.skillsync.auth.dto.RegisterRequest;
import in.skillsync.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * All endpoints are open — no JWT required.
 * Base path: /auth
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account. Role must be ROLE_LEARNER, ROLE_MENTOR, or ROLE_ADMIN"
    )

    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login and get JWT token",
        description = "Authenticate with email and password. Returns access and refresh tokens."
    )
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    @GetMapping("/internal/email/{userId}")
    public ResponseEntity<String> getEmailById(@PathVariable Long userId) {
        // This calls your existing AuthService/UserRepository
        String email = authService.getUserEmailById(userId); 
        return ResponseEntity.ok(email);
    }
}
