package com.multilingual.chat.app.dto;

import java.time.LocalDateTime;

public class MessageResponseDto {

    private Long id;
    private Long senderId;
    // private String senderName;
    private Long receiverId;
    // private String receiverName;
    private String originalText;
    private String translatedText;
    private String originalLanguage;
    private String targetLanguage;
    private LocalDateTime timestamp;

    public MessageResponseDto() {
    }

    public MessageResponseDto(Long id, Long senderId, // String senderName,
            Long receiverId, // String receiverName,
            String originalText, String translatedText,
            String originalLanguage, String targetLanguage,
            LocalDateTime timestamp) {
        this.id = id;
        this.senderId = senderId;
        // this.senderName = senderName;
        this.receiverId = receiverId;
        // this.receiverName = receiverName;
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.originalLanguage = originalLanguage;
        this.targetLanguage = targetLanguage;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    //

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
