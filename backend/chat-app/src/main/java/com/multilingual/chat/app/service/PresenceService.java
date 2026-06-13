package com.multilingual.chat.app.service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Tracks which users are currently connected, backed by Redis so the view is
 * shared across every backend instance.
 *
 * Previously this was an in-memory ConcurrentHashMap — fine for one instance, but
 * with multiple instances each only knew about its own connections, so "is user X
 * online?" gave the wrong answer whenever X was connected to a different instance.
 *
 * Redis layout (a user may have several sessions — multiple tabs/devices):
 *   ws:session:{sessionId}  -> "{userId}"        (reverse lookup on disconnect)
 *   ws:user:{userId}        -> Set<sessionId>    (a user is online iff this set is non-empty)
 *
 * Redis auto-deletes a set when its last member is removed, so the mere existence
 * of a ws:user:{id} key means that user is online.
 */
@Service
public class PresenceService {

    private static final String SESSION_KEY = "ws:session:";
    private static final String USER_KEY    = "ws:user:";

    // Safety net: if an instance crashes without firing disconnect events, these keys
    // expire on their own rather than leaving a user stuck "online" forever.
    private static final Duration TTL = Duration.ofHours(12);

    private final StringRedisTemplate redis;

    public PresenceService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void connect(String sessionId, Long userId) {
        redis.opsForValue().set(SESSION_KEY + sessionId, userId.toString(), TTL);
        redis.opsForSet().add(USER_KEY + userId, sessionId);
        redis.expire(USER_KEY + userId, TTL);
    }

    public Optional<Long> disconnect(String sessionId) {
        String userId = redis.opsForValue().get(SESSION_KEY + sessionId);
        if (userId == null) return Optional.empty();
        redis.delete(SESSION_KEY + sessionId);
        redis.opsForSet().remove(USER_KEY + userId, sessionId);
        return Optional.of(Long.valueOf(userId));
    }

    public boolean isOnline(Long userId) {
        Long size = redis.opsForSet().size(USER_KEY + userId);
        return size != null && size > 0;
    }

    public Set<Long> onlineUserIds() {
        Set<String> keys = redis.keys(USER_KEY + "*");
        if (keys == null) return Set.of();
        return keys.stream()
                .map(k -> k.substring(USER_KEY.length()))
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }
}
