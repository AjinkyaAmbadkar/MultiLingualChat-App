package com.multilingual.chat.app.dto;

import java.util.List;

public class ChatHistoryResponseDto {

    private Long user1Id;
    private Long user2Id;

    private List<MessageResponseDto> messages;

    public ChatHistoryResponseDto() {

    }

    public ChatHistoryResponseDto(Long user1Id, Long user2Id, List<MessageResponseDto> messages) {
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.messages = messages;
    }

    public Long getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }

    public Long getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
    }

    public List<MessageResponseDto> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponseDto> messages) {
        this.messages = messages;
    }

}
