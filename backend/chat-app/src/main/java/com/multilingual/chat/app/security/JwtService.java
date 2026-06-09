package com.multilingual.chat.app.security;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Handles all JWT operations: creating tokens and validating them.
 *
 * JWT structure recap:
 *   Header.Payload.Signature
 *
 *   Header   — algorithm used (HS256)
 *   Payload  — claims: who the token is for (subject = email), when it expires
 *   Signature— HMAC of header+payload using our secret key
 *               → only WE can create valid tokens, but anyone can READ the payload
 *               → never put sensitive data (passwords etc.) in the payload
 *
 * We use JJWT (io.jsonwebtoken) 0.12.x — the modern fluent API.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    // ── Token generation ──────────────────────────────────────────────────────

    /**
     * Creates a signed JWT access token for the given email.
     *
     * .subject()    — who this token identifies (we use email as the identifier)
     * .issuedAt()   — when this token was created
     * .expiration() — when it stops being valid (now + 15 min)
     * .signWith()   — sign with our HMAC-SHA256 secret key
     * .compact()    — serialize to the compact "header.payload.signature" string
     */
    public String generateAccessToken(String email) {
        log.debug("Generating access token for email: {}", email);
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token validation ──────────────────────────────────────────────────────

    /**
     * Returns true if the token is:
     *   1. Signed with our key (not forged)
     *   2. Not expired
     *   3. Belongs to the given user (email matches)
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return extractedEmail.equals(email) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Claims extraction ─────────────────────────────────────────────────────

    /**
     * Extracts the email (stored as the JWT "subject") from a token.
     * Throws JwtException if the token is invalid or expired.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Generic claim extractor — applies any function to the claims map.
     * Example: extractClaim(token, Claims::getSubject) extracts the email.
     *          extractClaim(token, Claims::getExpiration) extracts the expiry date.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the token signature, then returns all claims.
     * Throws JwtException (expired, malformed, bad signature) if invalid.
     *
     * JJWT 0.12.x API:
     *   Jwts.parser()
     *       .verifyWith(key)  — set the key used to check the signature
     *       .build()
     *       .parseSignedClaims(token)  — parse + verify in one step
     *       .getPayload()              — get the claims map
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Converts the base64-encoded secret string into a cryptographic key.
     *
     * Why base64? Secret keys are raw bytes, not readable strings.
     * We store the key as a base64 string in the env var, then decode it here.
     *
     * Keys.hmacShaKeyFor() creates an HMAC-SHA key appropriate for JWT signing.
     * The key must be at least 256 bits (32 bytes) for HS256.
     * Generate one with: openssl rand -base64 32
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
