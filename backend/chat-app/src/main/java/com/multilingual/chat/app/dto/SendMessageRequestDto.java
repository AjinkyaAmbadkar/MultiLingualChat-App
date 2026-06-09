package com.multilingual.chat.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload the client sends when posting a chat message.
 *
 * Phase 6 change: senderId REMOVED.
 *
 * Before Phase 6, the client told the server "I am user 5" — a security hole:
 * any authenticated user could impersonate any other user by forging senderId in the payload.
 *
 * Now the server derives the sender's identity from the JWT Principal:
 *   - WebSocket path: Principal set by JwtChannelInterceptor at STOMP CONNECT time
 *   - REST path:      Authentication set by JwtAuthFilter on every HTTP request
 *
 * The client only needs to supply what it legitimately knows:
 *   receiverId — who to message
 *   originalText / originalLanguage / targetLanguage — the message content
 */
public class SendMessageRequestDto {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotBlank(message = "Original text is required")
    private String originalText;

    // translatedText removed in a previous phase — the server calls OpenAI automatically.
    // senderId removed in Phase 6 — the server derives it from the JWT Principal.

    @NotBlank(message = "Original language is required")
    private String originalLanguage;

    @NotBlank(message = "Target language is required")
    private String targetLanguage;

    public SendMessageRequestDto() {
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
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
}
