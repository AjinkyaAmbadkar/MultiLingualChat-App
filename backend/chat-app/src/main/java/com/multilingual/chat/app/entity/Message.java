package com.multilingual.chat.app.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // ── Encrypted payload columns (Phase 8.5) ─────────────────────────────────
    // Plaintext (original_text, translated_text, sender_translated_text) removed.
    // All text is AES-256-GCM encrypted; AES key wrapped with recipient RSA public key.

    // nullable=true here so Hibernate's `update` DDL mode can ADD these columns to existing tables.
    // All new rows written by MessageService will always populate these fields.

    @Column(name = "encrypted_original_text", columnDefinition = "TEXT")
    private String encryptedOriginalText;

    @Column(name = "encrypted_translated_text", columnDefinition = "TEXT")
    private String encryptedTranslatedText;

    @Column(name = "encrypted_sender_text", columnDefinition = "TEXT")
    private String encryptedSenderText;

    /** RSA-OAEP wrapped AES key for the sender (Base64). */
    @Column(name = "aes_key_for_sender", columnDefinition = "TEXT")
    private String aesKeyForSender;

    /** RSA-OAEP wrapped AES key for the receiver (Base64). */
    @Column(name = "aes_key_for_receiver", columnDefinition = "TEXT")
    private String aesKeyForReceiver;

    /** Base64-encoded GCM nonce for encrypted_original_text. */
    @Column(name = "aes_iv_original", length = 32)
    private String aesIvOriginal;

    /** Base64-encoded GCM nonce for encrypted_translated_text. */
    @Column(name = "aes_iv_translated", length = 32)
    private String aesIvTranslated;

    /** Base64-encoded GCM nonce for encrypted_sender_text. */
    @Column(name = "aes_iv_sender", length = 32)
    private String aesIvSender;

    @Column(name = "original_language", nullable = false)
    private String originalLanguage;

    @Column(name = "target_language", nullable = false)
    private String targetLanguage;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public Message() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getEncryptedOriginalText() {
        return encryptedOriginalText;
    }

    public void setEncryptedOriginalText(String encryptedOriginalText) {
        this.encryptedOriginalText = encryptedOriginalText;
    }

    public String getEncryptedTranslatedText() {
        return encryptedTranslatedText;
    }

    public void setEncryptedTranslatedText(String encryptedTranslatedText) {
        this.encryptedTranslatedText = encryptedTranslatedText;
    }

    public String getEncryptedSenderText() {
        return encryptedSenderText;
    }

    public void setEncryptedSenderText(String encryptedSenderText) {
        this.encryptedSenderText = encryptedSenderText;
    }

    public String getAesKeyForSender() {
        return aesKeyForSender;
    }

    public void setAesKeyForSender(String aesKeyForSender) {
        this.aesKeyForSender = aesKeyForSender;
    }

    public String getAesKeyForReceiver() {
        return aesKeyForReceiver;
    }

    public void setAesKeyForReceiver(String aesKeyForReceiver) {
        this.aesKeyForReceiver = aesKeyForReceiver;
    }

    public String getAesIvOriginal() {
        return aesIvOriginal;
    }

    public void setAesIvOriginal(String aesIvOriginal) {
        this.aesIvOriginal = aesIvOriginal;
    }

    public String getAesIvTranslated() {
        return aesIvTranslated;
    }

    public void setAesIvTranslated(String aesIvTranslated) {
        this.aesIvTranslated = aesIvTranslated;
    }

    public String getAesIvSender() {
        return aesIvSender;
    }

    public void setAesIvSender(String aesIvSender) {
        this.aesIvSender = aesIvSender;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
