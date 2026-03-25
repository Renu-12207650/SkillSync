package in.skillsync.auth.security;

import in.skillsync.auth.entity.AuthUser;
import in.skillsync.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads user details by email for Spring Security authentication.
 * Used by DaoAuthenticationProvider during login.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthUserRepository authUserRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AuthUser authUser = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        return new User(
                authUser.getEmail(),
                authUser.getPassword(),
                authUser.isEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority(authUser.getRole().name()))
        );
    }
}
