package com.multilingual.chat.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multilingual.chat.app.dto.AuthResponseDto;
import com.multilingual.chat.app.dto.LoginRequestDto;
import com.multilingual.chat.app.dto.RefreshTokenRequestDto;
import com.multilingual.chat.app.dto.RegisterRequestDto;
import com.multilingual.chat.app.service.AuthService;

import jakarta.validation.Valid;

/**
 * All authentication endpoints — all under /auth/** (publicly accessible, no token needed).
 *
 * POST /auth/register  — create a new account → returns access + refresh token
 * POST /auth/login     — log in with email+password → returns access + refresh token
 * POST /auth/refresh   — exchange refresh token for a new access token
 * POST /auth/logout    — revoke the refresh token
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     * Returns 201 CREATED with tokens so the user is immediately logged in after signup.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        log.info("POST /auth/register | email: {}", request.getEmail());
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Log in with email + password.
     * Returns 200 OK with new access + refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("POST /auth/login | email: {}", request.getEmail());
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Exchange a refresh token for a new access token.
     * Call this when the access token expires (you'll get a 401 on API calls).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("POST /auth/refresh");
        AuthResponseDto response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke the refresh token — effectively logs the user out.
     * Returns 204 NO CONTENT (success with no body to return).
     *
     * Note: the access token still works for up to 15 minutes after logout.
     * This is the accepted trade-off with stateless JWT.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        log.info("POST /auth/logout");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
