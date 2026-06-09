package com.multilingual.chat.app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.UserRepository;

import java.util.List;

/**
 * Bridges our User entity to Spring Security's authentication system.
 *
 * Spring Security doesn't know about our User entity — it works with a
 * UserDetails interface. This service is the translator between the two.
 *
 * Spring Security calls loadUserByUsername() automatically during:
 *   1. login (to verify the password against the stored hash)
 *   2. every request (via JwtAuthFilter, to load the user from the token's email)
 *
 * "Username" in Spring Security = email address in our app.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user from DB by email and wraps it in Spring Security's UserDetails.
     *
     * We use Spring Security's built-in User builder (note: different from our entity User)
     * to create a UserDetails object with:
     *   - username  = email (what Spring Security uses as the identifier)
     *   - password  = the bcrypt hash (Spring Security compares this during login)
     *   - authorities = roles (we give everyone "ROLE_USER" for now)
     *
     * Google OAuth users have no password — we pass an empty string so Spring Security
     * doesn't NPE, but they'll never authenticate via password anyway.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        return new org.springframework.security.core.userdetails.User(
                user.getemail(),
                user.getPasswordHash() != null ? user.getPasswordHash() : "",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
