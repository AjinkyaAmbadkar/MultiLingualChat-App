package com.multilingual.chat.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload the client sends when posting a chat message.
 *
 * Phase 6:  senderId removed — server derives sender from JWT Principal.
 * Phase 7:  originalLanguage + targetLanguage removed — server derives both
 *           from sender.preferredLanguage and receiver.preferredLanguage stored in DB.
 *
 * The client now only needs to supply the bare minimum:
 *   receiverId   — who to send to
 *   originalText — what to say
 *
 * The server handles everything else: who sent it, what languages to use,
 * whether translation is needed, and calling OpenAI only when languages differ.
 */
public class SendMessageRequestDto {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotBlank(message = "Original text is required")
    private String originalText;

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
}
