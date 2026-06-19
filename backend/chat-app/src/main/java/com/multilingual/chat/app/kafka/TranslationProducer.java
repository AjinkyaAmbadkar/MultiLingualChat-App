package com.multilingual.chat.app.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.config.KafkaTopicConfig;
import com.multilingual.chat.app.dto.TranslationJobDto;

@Service
public class TranslationProducer {

    private static final Logger log = LoggerFactory.getLogger(TranslationProducer.class);

    private final KafkaTemplate<String, TranslationJobDto> kafkaTemplate;

    public TranslationProducer(KafkaTemplate<String, TranslationJobDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTranslationJob(TranslationJobDto job) {
        // Use receiverId as partition key so messages to the same receiver stay ordered
        kafkaTemplate.send(KafkaTopicConfig.TRANSLATION_TOPIC,
                String.valueOf(job.getReceiverId()), job);
        log.info("[Kafka] Translation job published | messageId: {}", job.getMessageId());
    }
}
