package com.multilingual.chat.app.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Intercepts every HTTP request and checks for a valid JWT in the Authorization header.
 *
 * Extends OncePerRequestFilter — Spring guarantees this filter runs exactly ONCE
 * per request, never twice (important for filter chains with redirects).
 *
 * Request flow through this filter:
 *
 *   1. Read the "Authorization" header
 *   2. If missing or not "Bearer ..." → skip (let other filters handle it)
 *   3. Extract the token string after "Bearer "
 *   4. Extract email from token via JwtService
 *   5. If email is valid and no authentication set yet:
 *        a. Load UserDetails from DB
 *        b. Validate token (signature + expiry + email match)
 *        c. If valid → set authentication in SecurityContext
 *   6. Pass request to the next filter regardless
 *
 * Setting auth in SecurityContext is what tells Spring Security
 * "this request is authenticated — let it through".
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Authorization header or not a Bearer token → skip JWT processing entirely.
        // Public endpoints (login, register, WebSocket) fall through here naturally.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix (7 characters) to get the raw JWT string
        String token = authHeader.substring(7);
        String email;

        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            // Malformed token, expired, bad signature — treat as unauthenticated
            log.warn("Failed to extract email from JWT: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Only set authentication if:
        //   - we successfully extracted an email
        //   - the SecurityContext doesn't already have auth (avoid re-processing)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                /*
                 * Create an authentication token and put it in the SecurityContext.
                 *
                 * UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                 *   principal   = UserDetails object (who is authenticated)
                 *   credentials = null (we don't need the password after verification)
                 *   authorities = roles (ROLE_USER)
                 *
                 * Passing authorities (3-arg constructor) marks this as authenticated.
                 * The 2-arg constructor (no authorities) marks it as NOT authenticated.
                 */
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP, session) — useful for audit logs
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("JWT authenticated user: {}", email);
            } else {
                log.warn("JWT token validation failed for email: {}", email);
            }
        }

        // Always continue the filter chain — SecurityConfig decides what to block
        filterChain.doFilter(request, response);
    }
}
