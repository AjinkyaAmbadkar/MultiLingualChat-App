package com.multilingual.chat.app.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import com.multilingual.chat.app.dto.AuthResponseDto;
import com.multilingual.chat.app.dto.LoginRequestDto;
import com.multilingual.chat.app.dto.RegisterRequestDto;
import com.multilingual.chat.app.entity.AuthProvider;
import com.multilingual.chat.app.entity.RefreshToken;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.RefreshTokenRepository;
import com.multilingual.chat.app.repository.UserRepository;
import com.multilingual.chat.app.security.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${google.client-id}")
    private String googleClientId;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Creates a new LOCAL user account and returns tokens immediately.
     * (No email verification in this phase — isVerified stays false)
     *
     * @Transactional ensures both the User save and RefreshToken save
     * happen in the same DB transaction. If either fails, both roll back.
     */
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // never store plain text
        user.setPreferredLanguage(request.getPreferredLanguage());
        user.setProvider(AuthProvider.LOCAL);
        user.setVerified(false);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully | userId: {}", savedUser.getId());

        return buildAuthResponse(savedUser);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates credentials and issues new tokens.
     *
     * authenticationManager.authenticate() does two things internally:
     *   1. Calls UserDetailsServiceImpl.loadUserByUsername(email) → fetches user from DB
     *   2. Calls passwordEncoder.matches(rawPassword, storedHash) → verifies password
     *
     * If either fails it throws BadCredentialsException → Spring returns 401.
     * We don't need to manually check the password — the AuthenticationManager handles it.
     */
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Throws BadCredentialsException if email not found or password wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Revoke all existing refresh tokens before issuing a new one.
        // This means logging in on a new device logs out all other devices.
        // Remove this line if you want to support multiple concurrent sessions.
        refreshTokenRepository.deleteAllByUser(user);

        log.info("Login successful | userId: {}", user.getId());
        return buildAuthResponse(user);
    }

    // ── Google OAuth2 Login ───────────────────────────────────────────────────

    /**
     * Verifies a Google ID Token and issues our own JWT pair.
     *
     * Flow:
     *   1. Call Google's tokeninfo endpoint to verify the token is authentic
     *   2. Check that the token was issued for OUR app (aud == our client ID)
     *   3. Check email_verified is true (prevents unverified Gmail accounts)
     *   4. If the email already exists as a LOCAL account → reject (no silent linking)
     *   5. If the email exists as a GOOGLE account → log them in (return JWT)
     *   6. Otherwise → create a new GOOGLE user and log them in
     *
     * We use Google's tokeninfo endpoint (simple HTTP call, no extra library).
     * Alternative: use the google-api-client library for offline verification
     * (no network call) — but tokeninfo is simpler and sufficient for now.
     */
    @Transactional
    public AuthResponseDto loginWithGoogle(String idToken) {
        log.info("Google OAuth2 login attempt");

        // ── Step 1: Verify the token with Google ─────────────────────────────
        Map<?, ?> tokenInfo = RestClient.create()
                .get()
                .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                .retrieve()
                .body(Map.class);

        if (tokenInfo == null) {
            log.warn("Google tokeninfo returned null");
            throw new RuntimeException("Failed to verify Google token");
        }

        // ── Step 2: Verify audience (aud) matches our Client ID ───────────────
        // This prevents a token issued for another app from being used here.
        String aud = (String) tokenInfo.get("aud");
        if (!googleClientId.equals(aud)) {
            log.warn("Google token aud mismatch — expected: {} got: {}", googleClientId, aud);
            throw new RuntimeException("Google token was not issued for this application");
        }

        // ── Step 3: Verify email is confirmed by Google ───────────────────────
        String emailVerified = (String) tokenInfo.get("email_verified");
        if (!"true".equals(emailVerified)) {
            log.warn("Google login rejected — email not verified");
            throw new RuntimeException("Google account email is not verified");
        }

        // ── Extract user info from the token ──────────────────────────────────
        String email   = (String) tokenInfo.get("email");
        String name    = (String) tokenInfo.get("name");
        String picture = (String) tokenInfo.get("picture");
        String googleId = (String) tokenInfo.get("sub"); // "sub" is Google's unique user ID

        log.info("Google token verified | email: {}", email);

        // ── Step 4 & 5: Find existing user or create new ──────────────────────
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();

            // Block: email registered via LOCAL provider — prevent silent account takeover
            if (user.getProvider() == AuthProvider.LOCAL) {
                log.warn("Google login blocked — email {} already registered as LOCAL", email);
                throw new RuntimeException(
                    "This email is already registered with a password. Please log in with your password instead.");
            }

            // Existing GOOGLE user — update picture in case it changed
            user.setPictureUrl(picture);
            user = userRepository.save(user);
            log.info("Existing Google user logged in | userId: {}", user.getId());

        } else {
            // ── Step 6: New Google user — create account ──────────────────────
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setGoogleId(googleId);
            user.setPictureUrl(picture);
            user.setProvider(AuthProvider.GOOGLE);
            user.setVerified(true);           // Google has already verified their email
            user.setPreferredLanguage("en");  // Default — user can change later

            // No passwordHash for Google users — they authenticate via Google, not a password
            user = userRepository.save(user);
            log.info("New Google user created | userId: {}", user.getId());
        }

        // Revoke existing refresh tokens (same single-session policy as password login)
        refreshTokenRepository.deleteAllByUser(user);

        return buildAuthResponse(user);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * Validates a refresh token and issues a new access token.
     *
     * We do NOT rotate the refresh token here (the same refresh token is returned).
     * For higher security, you could issue a new refresh token and revoke the old one
     * on every refresh (refresh token rotation) — but that's a Phase 3 concern.
     */
    @Transactional
    public AuthResponseDto refreshAccessToken(String refreshTokenString) {
        log.info("Refresh token request received");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            log.warn("Attempted use of revoked refresh token");
            throw new RuntimeException("Refresh token has been revoked. Please log in again.");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Attempted use of expired refresh token");
            throw new RuntimeException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getemail());
        log.info("Access token refreshed for userId: {}", user.getId());

        return new AuthResponseDto(newAccessToken, refreshTokenString, user.getId(), user.getemail(), user.getname());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Revokes the provided refresh token.
     *
     * After logout, the access token technically still works until it expires (15 min).
     * This is the accepted trade-off with stateless JWT — you can't revoke access tokens.
     * The short expiry window (15 min) limits the damage window.
     *
     * If you need immediate access token invalidation, you'd need a token blacklist
     * (stored in Redis) — that's a future enhancement.
     */
    @Transactional
    public void logout(String refreshTokenString) {
        log.info("Logout request received");

        refreshTokenRepository.findByToken(refreshTokenString).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for userId: {}", token.getUser().getId());
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Generates both tokens and packages them with user info into the response.
     * Called after both register and login.
     */
    private AuthResponseDto buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user.getemail());
        String refreshToken = createAndSaveRefreshToken(user);
        return new AuthResponseDto(accessToken, refreshToken, user.getId(), user.getemail(), user.getname());
    }

    /**
     * Creates a RefreshToken entity and persists it.
     *
     * The token value is a random UUID — opaque to the client, just a lookup key.
     * Expiry = now + refresh-token-expiration (7 days, in milliseconds → converted to seconds).
     */
    private String createAndSaveRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = new RefreshToken(user, tokenValue, expiresAt);
        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token created for userId: {} | expires: {}", user.getId(), expiresAt);
        return tokenValue;
    }
}
