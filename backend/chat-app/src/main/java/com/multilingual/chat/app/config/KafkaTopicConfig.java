package com.multilingual.chat.app.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String TRANSLATION_TOPIC = "translation-jobs";

    @Bean
    public NewTopic translationJobsTopic() {
        return TopicBuilder.name(TRANSLATION_TOPIC)
                .partitions(3)   // 3 partitions = up to 3 parallel consumer threads
                .replicas(1)     // 1 replica (single broker setup)
                .build();
    }
}
