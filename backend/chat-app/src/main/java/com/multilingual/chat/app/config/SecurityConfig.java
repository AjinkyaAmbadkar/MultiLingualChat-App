package com.multilingual.chat.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.multilingual.chat.app.security.JwtAuthFilter;

/**
 * Central Spring Security configuration.
 *
 * Spring Security 6+ uses the SecurityFilterChain bean approach.
 * The old WebSecurityConfigurerAdapter is completely removed — don't use it.
 *
 * Three beans defined here:
 *   1. SecurityFilterChain — the rules: who gets in, who gets blocked
 *   2. PasswordEncoder     — BCrypt for hashing passwords
 *   3. AuthenticationManager — used by AuthService to validate login credentials
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Defines the security filter chain — the core of Spring Security config.
     *
     * csrf        — disabled because we use JWT (stateless). CSRF protection is
     *               only needed for session-cookie based auth where a malicious site
     *               can piggyback on the browser's cookie. JWT in Authorization header
     *               is not sent automatically by the browser, so CSRF is not a threat.
     *
     * sessionManagement — STATELESS means Spring never creates an HttpSession.
     *                     Each request must authenticate itself via JWT. No server-side
     *                     session state is maintained.
     *
     * authorizeHttpRequests — defines which URLs are public and which require auth:
     *   /auth/**         → permitAll: login, register, refresh, logout (no token needed)
     *   /ws/**           → permitAll: WebSocket handshake happens before JWT is available
     *   /chat-test.html  → permitAll: the HTML test page is a static file
     *   /test            → permitAll: health check endpoint
     *   anyRequest       → authenticated: everything else needs a valid JWT
     *
     * addFilterBefore  — insert our JwtAuthFilter BEFORE Spring's default
     *                    UsernamePasswordAuthenticationFilter. This ensures the JWT is
     *                    checked and the SecurityContext is populated before any
     *                    authorization decisions are made.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/chat-test.html").permitAll()
                .requestMatchers("/test").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder hashes passwords using the bcrypt algorithm.
     *
     * BCrypt automatically:
     *   - Generates a random salt per password (same password → different hash each time)
     *   - Applies the algorithm multiple times (work factor = 10 by default)
     *   - Makes brute-force attacks expensive
     *
     * Usage in AuthService:
     *   passwordEncoder.encode("rawPassword")           → stores in DB
     *   passwordEncoder.matches("rawPassword", hash)    → verifies on login
     *
     * Never store plain text passwords. Never use MD5 or SHA-1 for passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the AuthenticationManager bean.
     *
     * AuthenticationManager is what actually validates credentials during login.
     * Spring Boot auto-configures one that uses our UserDetailsServiceImpl +
     * PasswordEncoder. We just expose it as a bean so AuthService can inject it.
     *
     * AuthService calls: authenticationManager.authenticate(token)
     * → Spring Security calls: UserDetailsServiceImpl.loadUserByUsername(email)
     * → then: passwordEncoder.matches(rawPassword, storedHash)
     * → throws BadCredentialsException if wrong, returns Authentication if correct
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
