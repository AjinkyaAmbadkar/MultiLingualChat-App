package com.multilingual.chat.app.dto;

/**
 * Payload published to the Kafka "translation-jobs" topic.
 *
 * Includes originalText in plaintext — consistent with the existing security model
 * where the server already handles plaintext during the translation step.
 * The consumer translates, encrypts, saves, and delivers to the receiver.
 */
public class TranslationJobDto {

    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String originalText;
    private String senderLanguage;
    private String receiverLanguage;
    private String senderPublicKey;
    private String receiverPublicKey;

    public TranslationJobDto() {}

    public TranslationJobDto(Long messageId, Long senderId, Long receiverId,
                             String originalText, String senderLanguage, String receiverLanguage,
                             String senderPublicKey, String receiverPublicKey) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.originalText = originalText;
        this.senderLanguage = senderLanguage;
        this.receiverLanguage = receiverLanguage;
        this.senderPublicKey = senderPublicKey;
        this.receiverPublicKey = receiverPublicKey;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getSenderLanguage() { return senderLanguage; }
    public void setSenderLanguage(String senderLanguage) { this.senderLanguage = senderLanguage; }

    public String getReceiverLanguage() { return receiverLanguage; }
    public void setReceiverLanguage(String receiverLanguage) { this.receiverLanguage = receiverLanguage; }

    public String getSenderPublicKey() { return senderPublicKey; }
    public void setSenderPublicKey(String senderPublicKey) { this.senderPublicKey = senderPublicKey; }

    public String getReceiverPublicKey() { return receiverPublicKey; }
    public void setReceiverPublicKey(String receiverPublicKey) { this.receiverPublicKey = receiverPublicKey; }
}
