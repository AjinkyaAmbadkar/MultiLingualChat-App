package com.multilingual.chat.app.service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PresenceService {

    // sessionId → userId
    private final ConcurrentHashMap<String, Long> sessions = new ConcurrentHashMap<>();

    public void connect(String sessionId, Long userId) {
        sessions.put(sessionId, userId);
    }

    public Optional<Long> disconnect(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }

    public boolean isOnline(Long userId) {
        return sessions.containsValue(userId);
    }

    public Set<Long> onlineUserIds() {
        return Collections.unmodifiableSet(ConcurrentHashMap.newKeySet()) ;
    }
}
