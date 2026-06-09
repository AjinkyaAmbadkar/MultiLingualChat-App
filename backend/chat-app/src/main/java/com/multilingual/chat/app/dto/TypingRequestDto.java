package com.multilingual.chat.app.dto;

public class TypingRequestDto {

    private Long receiverId;
    private boolean typing;

    public TypingRequestDto() {}

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public boolean isTyping() { return typing; }
    public void setTyping(boolean typing) { this.typing = typing; }
}
