package com.multilingual.chat.app.dto;

import java.time.LocalDateTime;

/**
 * Represents one conversation entry in the sidebar list.
 * Contains the other participant's info + the last message preview.
 */
public class ConversationDto {

    private Long userId;
    private String name;
    private String pictureUrl;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;

    public ConversationDto() {}

    public ConversationDto(Long userId, String name, String pictureUrl,
                           String lastMessage, LocalDateTime lastMessageTime,
                           long unreadCount) {
        this.userId          = userId;
        this.name            = name;
        this.pictureUrl      = pictureUrl;
        this.lastMessage     = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount     = unreadCount;
    }

    public Long getUserId()                  { return userId; }
    public String getName()                  { return name; }
    public String getPictureUrl()            { return pictureUrl; }
    public String getLastMessage()           { return lastMessage; }
    public LocalDateTime getLastMessageTime(){ return lastMessageTime; }
    public long getUnreadCount()             { return unreadCount; }
}
