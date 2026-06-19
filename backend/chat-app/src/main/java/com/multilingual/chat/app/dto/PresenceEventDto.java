package com.multilingual.chat.app.dto;

public class PresenceEventDto {

    private final String type = "PRESENCE";
    private Long userId;
    private boolean online;

    public PresenceEventDto() {}

    public PresenceEventDto(Long userId, boolean online) {
        this.userId = userId;
        this.online = online;
    }

    public String getType()    { return type; }
    public Long getUserId()    { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public boolean isOnline()  { return online; }
    public void setOnline(boolean online) { this.online = online; }
}
