package com.multilingual.chat.app.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Stores JWT refresh tokens issued to users.
 *
 * Why a separate table?
 * Access tokens are short-lived (15 min) and stateless — we never store them.
 * Refresh tokens are long-lived (7–30 days) and MUST be stored so we can:
 * - Validate a refresh request ("is this token real and not expired?")
 * - Revoke a specific token on logout
 * - Revoke ALL tokens for a user (e.g. on password change or suspicious
 * activity)
 *
 * One user can have multiple active refresh tokens (multiple devices/browsers).
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user this token belongs to.
     * FetchType.LAZY = don't load the User row unless we explicitly access it.
     * This avoids an unnecessary JOIN every time we look up a token.
     * ON DELETE CASCADE is handled in SQL — if the user is deleted, their tokens go
     * too.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The actual token string — a secure random UUID or JWT, stored as-is.
     * Must be unique across all rows (enforced at DB level too via the index).
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * When this token expires. Checked on every refresh request.
     * Expired tokens are rejected even if not explicitly revoked.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Explicit revocation flag for immediate invalidation (e.g. user logs out).
     * Using a flag instead of deletion makes audit trails easier.
     * Defaults to false — tokens are valid until they expire or are revoked.
     */
    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public RefreshToken() {
    }

    public RefreshToken(User user, String token, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    // ── JPA lifecycle hook ────────────────────────────────────────────────────

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
