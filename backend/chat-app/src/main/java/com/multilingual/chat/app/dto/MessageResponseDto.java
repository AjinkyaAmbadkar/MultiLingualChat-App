package com.multilingual.chat.app.dto;

import java.time.LocalDateTime;

/**
 * Message payload sent to clients.
 *
 * Phase 8.5: plaintext fields replaced with encrypted blobs.
 * The client decrypts using its in-memory RSA private key:
 *   - Receiver: unwrap aesKeyForReceiver → decrypt encryptedTranslatedText with aesIvTranslated
 *   - Sender:   unwrap aesKeyForSender   → decrypt encryptedOriginalText    with aesIvOriginal
 *
 * Trust boundary: server holds plaintext only during translation (OpenAI call), never persists it.
 */
public class MessageResponseDto {

    private Long id;
    private Long senderId;
    private Long receiverId;

    // ── Encrypted blobs (receiver's perspective) ──────────────────────────────
    private String encryptedTranslatedText;
    private String aesKeyForReceiver;
    private String aesIvTranslated;

    // ── Encrypted blobs (sender's perspective) ────────────────────────────────
    private String encryptedOriginalText;
    private String encryptedSenderText;
    private String aesKeyForSender;
    private String aesIvOriginal;
    private String aesIvSender;

    private String originalLanguage;
    private String targetLanguage;

    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime timestamp;

    public MessageResponseDto() {
    }

    public MessageResponseDto(Long id, Long senderId, Long receiverId,
                               String encryptedOriginalText, String encryptedTranslatedText,
                               String encryptedSenderText,
                               String aesKeyForSender, String aesKeyForReceiver,
                               String aesIvOriginal, String aesIvTranslated, String aesIvSender,
                               String originalLanguage, String targetLanguage,
                               boolean isRead, LocalDateTime timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.encryptedOriginalText = encryptedOriginalText;
        this.encryptedTranslatedText = encryptedTranslatedText;
        this.encryptedSenderText = encryptedSenderText;
        this.aesKeyForSender = aesKeyForSender;
        this.aesKeyForReceiver = aesKeyForReceiver;
        this.aesIvOriginal = aesIvOriginal;
        this.aesIvTranslated = aesIvTranslated;
        this.aesIvSender = aesIvSender;
        this.originalLanguage = originalLanguage;
        this.targetLanguage = targetLanguage;
        this.isRead = isRead;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getEncryptedTranslatedText() { return encryptedTranslatedText; }
    public void setEncryptedTranslatedText(String encryptedTranslatedText) { this.encryptedTranslatedText = encryptedTranslatedText; }

    public String getAesKeyForReceiver() { return aesKeyForReceiver; }
    public void setAesKeyForReceiver(String aesKeyForReceiver) { this.aesKeyForReceiver = aesKeyForReceiver; }

    public String getAesIvTranslated() { return aesIvTranslated; }
    public void setAesIvTranslated(String aesIvTranslated) { this.aesIvTranslated = aesIvTranslated; }

    public String getEncryptedOriginalText() { return encryptedOriginalText; }
    public void setEncryptedOriginalText(String encryptedOriginalText) { this.encryptedOriginalText = encryptedOriginalText; }

    public String getEncryptedSenderText() { return encryptedSenderText; }
    public void setEncryptedSenderText(String encryptedSenderText) { this.encryptedSenderText = encryptedSenderText; }

    public String getAesKeyForSender() { return aesKeyForSender; }
    public void setAesKeyForSender(String aesKeyForSender) { this.aesKeyForSender = aesKeyForSender; }

    public String getAesIvOriginal() { return aesIvOriginal; }
    public void setAesIvOriginal(String aesIvOriginal) { this.aesIvOriginal = aesIvOriginal; }

    public String getAesIvSender() { return aesIvSender; }
    public void setAesIvSender(String aesIvSender) { this.aesIvSender = aesIvSender; }

    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
