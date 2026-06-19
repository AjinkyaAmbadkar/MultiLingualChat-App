package com.multilingual.chat.app.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Existing fields (unchanged) ───────────────────────────────────────────

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage;

    // ── Auth fields (Phase 1 addition) ───────────────────────────────────────

    /**
     * Bcrypt hash of the user's password.
     * NULL for Google OAuth users — they authenticate via Google, not a password.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * How the user signed up: LOCAL (email+password) or GOOGLE (OAuth2).
     * EnumType.STRING stores "LOCAL" / "GOOGLE" as readable text, not an integer.
     * Defaults to LOCAL so existing users and new sign-ups without OAuth work correctly.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * The unique ID returned by Google for this user.
     * NULL for LOCAL users. Used to look up a Google user on OAuth2 login.
     */
    @Column(name = "google_id")
    private String googleId;

    /**
     * Profile picture URL from Google.
     * NULL for LOCAL users (unless they upload one in a future phase).
     * Populated from the Google ID token's "picture" claim on OAuth2 login.
     */
    @Column(name = "picture_url")
    private String pictureUrl;

    /**
     * Whether this user's email address has been verified.
     * LOCAL users must verify via email link; Google users are pre-verified.
     */
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated every time this entity is saved (via @PreUpdate).
     * Useful for auditing: "when was this user's profile last changed?"
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Encryption keypair fields (Phase 8.5) ─────────────────────────────────

    /** Base64-encoded X.509 RSA-2048 public key. */
    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    /** AES-256-GCM encrypted PKCS#8 private key, Base64-encoded. */
    @Column(name = "encrypted_private_key", columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    /** Base64-encoded 16-byte PBKDF2 salt used to derive the key-encryption key. */
    @Column(name = "key_salt", length = 64)
    private String keySalt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public User() {
    }

    /** Kept for backwards compatibility — existing code that uses this constructor still compiles. */
    public User(Long id, String name, String email, String preferredLanguage, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.preferredLanguage = preferredLanguage;
        this.createdAt = createdAt;
    }

    // ── JPA lifecycle hooks ───────────────────────────────────────────────────

    /**
     * Runs automatically BEFORE a new row is INSERTed.
     * Sets both timestamps on creation.
     */
    @PrePersist
    public void PrePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Runs automatically BEFORE an existing row is UPDATEd.
     * Only updates updatedAt — createdAt must never change after creation.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    // NOTE: existing non-standard names (getemail, getname, getcreatedAt) are kept
    // intentionally so UserService and UserController continue to compile without changes.

    public Long getId() {
        return id;
    }

    public String getname() {
        return name;
    }

    public String getemail() {
        return email;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public LocalDateTime getcreatedAt() {
        return createdAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getGoogleId() {
        return googleId;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getEncryptedPrivateKey() {
        return encryptedPrivateKey;
    }

    public void setEncryptedPrivateKey(String encryptedPrivateKey) {
        this.encryptedPrivateKey = encryptedPrivateKey;
    }

    public String getKeySalt() {
        return keySalt;
    }

    public void setKeySalt(String keySalt) {
        this.keySalt = keySalt;
    }
}
