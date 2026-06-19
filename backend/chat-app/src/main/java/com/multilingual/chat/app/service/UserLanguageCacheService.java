package com.multilingual.chat.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.repository.UserRepository;

/**
 * Caches userId → preferredLanguage using Caffeine (in-memory).
 *
 * Write-through: every language update hits DB first, then updates the cache.
 * Cache miss: falls back to DB and populates the cache for next time.
 * TTL: 60 minutes (safety net — entries are always updated on write).
 */
@Service
public class UserLanguageCacheService {

    private static final Logger log = LoggerFactory.getLogger(UserLanguageCacheService.class);

    private final UserRepository userRepository;

    public UserLanguageCacheService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the preferred language for the given userId.
     * On first call: hits DB and populates cache.
     * On subsequent calls: returns from Caffeine cache (no DB hit).
     */
    @Cacheable(value = "userLanguage", key = "#userId")
    public String getPreferredLanguage(Long userId) {
        log.debug("Cache miss for userId: {} — fetching language from DB", userId);
        return userRepository.findById(userId)
                .map(u -> u.getPreferredLanguage())
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    /**
     * Write-through: called after a language update is saved to DB.
     * Updates the cache entry so reads are immediately consistent.
     */
    @CachePut(value = "userLanguage", key = "#userId")
    public String updateCachedLanguage(Long userId, String newLanguage) {
        log.debug("Cache write-through for userId: {} → {}", userId, newLanguage);
        return newLanguage;
    }
}
