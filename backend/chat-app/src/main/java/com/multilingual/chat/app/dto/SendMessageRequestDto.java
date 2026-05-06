package com.multilingual.chat.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SendMessageRequestDto {

    @NotNull(message = "Sender ID is required")
    private Long senderId;

    @NotNull(message = "reciever ID is required")
    private Long receiverId;

    @NotBlank(message = "Original Text is required")
    private String originalText;

    // translatedText removed — the server now calls OpenAI to generate this automatically.
    // Clients only send the original message + source/target language.

    @NotBlank(message = "Original language is required")
    private String originalLanguage;

    @NotBlank(message = "Target language is required")
    private String targetLanguage;

    public SendMessageRequestDto() {

    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
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
