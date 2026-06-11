package com.multilingual.chat.app.entity;

public enum MessageStatus {
    PENDING,    // saved to DB, queued for translation
    DELIVERED,  // translated and pushed to receiver
    FAILED      // OpenAI call failed after retries
}
