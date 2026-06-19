package com.multilingual.chat.app.kafka;

import java.security.PublicKey;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.config.KafkaTopicConfig;
import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.TranslationJobDto;
import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.entity.MessageStatus;
import com.multilingual.chat.app.repository.MessageRepository;
import com.multilingual.chat.app.service.EncryptionService;
import com.multilingual.chat.app.service.TranslationService;

/**
 * Kafka consumer — processes translation jobs asynchronously.
 *
 * Flow per job:
 *   1. Translate originalText (OpenAI call — may take 1-2s, happens off the WS thread)
 *   2. Encrypt translated text + update DB (status → DELIVERED)
 *   3. Push completed message to receiver via WebSocket
 */
@Service
public class TranslationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TranslationConsumer.class);

    private final MessageRepository messageRepository;
    private final TranslationService translationService;
    private final EncryptionService encryptionService;
    private final SimpMessagingTemplate messagingTemplate;

    public TranslationConsumer(MessageRepository messageRepository,
                               TranslationService translationService,
                               EncryptionService encryptionService,
                               SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.translationService = translationService;
        this.encryptionService = encryptionService;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = KafkaTopicConfig.TRANSLATION_TOPIC,
                   groupId = "translation-group",
                   concurrency = "3")
    public void consume(TranslationJobDto job) {
        log.info("[Kafka] Processing job | messageId: {}", job.getMessageId());

        Message message = messageRepository.findById(job.getMessageId()).orElse(null);
        if (message == null || message.getStatus() != MessageStatus.PENDING) return;

        try {
            // Translate (OpenAI call happens here, off the WebSocket thread)
            String translatedText = translationService.isTranslationRequired(
                    job.getSenderLanguage(), job.getReceiverLanguage())
                    ? translationService.translate(job.getOriginalText(),
                            job.getSenderLanguage(), job.getReceiverLanguage())
                    : job.getOriginalText();

            // Generate one fresh AES key for all three ciphertext fields so the client
            // can decrypt original, translated, and senderText with a single unwrap.
            byte[] aesKey       = encryptionService.generateAesKey();
            byte[] ivOriginal   = encryptionService.generateAesIv();
            byte[] ivTranslated = encryptionService.generateAesIv();
            byte[] ivSender     = encryptionService.generateAesIv();

            String senderText   = job.getOriginalText(); // sender always sees what they typed

            PublicKey senderPk   = encryptionService.decodePublicKey(job.getSenderPublicKey());
            PublicKey receiverPk = encryptionService.decodePublicKey(job.getReceiverPublicKey());

            message.setEncryptedOriginalText(encryptionService.aesEncrypt(aesKey, ivOriginal, job.getOriginalText()));
            message.setEncryptedTranslatedText(encryptionService.aesEncrypt(aesKey, ivTranslated, translatedText));
            message.setEncryptedSenderText(encryptionService.aesEncrypt(aesKey, ivSender, senderText));
            message.setAesKeyForSender(encryptionService.rsaEncrypt(senderPk, aesKey));
            message.setAesKeyForReceiver(encryptionService.rsaEncrypt(receiverPk, aesKey));
            message.setAesIvOriginal(Base64.getEncoder().encodeToString(ivOriginal));
            message.setAesIvTranslated(Base64.getEncoder().encodeToString(ivTranslated));
            message.setAesIvSender(Base64.getEncoder().encodeToString(ivSender));
            message.setStatus(MessageStatus.DELIVERED);
            messageRepository.save(message);

            log.info("[Kafka] Job complete | messageId: {}", message.getId());

            // Push translated message to receiver; echo to sender so their UI gets server ID
            MessageResponseDto dto = mapToDto(message);
            messagingTemplate.convertAndSend("/topic/user." + job.getReceiverId(), dto);
            messagingTemplate.convertAndSend("/topic/user." + job.getSenderId(), dto);

        } catch (Exception e) {
            log.error("[Kafka] Job failed | messageId: {} | {}", job.getMessageId(), e.getMessage());
            message.setStatus(MessageStatus.FAILED);
            messageRepository.save(message);
        }
    }

    private MessageResponseDto mapToDto(Message m) {
        return new MessageResponseDto(
                m.getId(), m.getSender().getId(), m.getReceiver().getId(),
                m.getEncryptedOriginalText(), m.getEncryptedTranslatedText(), m.getEncryptedSenderText(),
                m.getAesKeyForSender(), m.getAesKeyForReceiver(),
                m.getAesIvOriginal(), m.getAesIvTranslated(), m.getAesIvSender(),
                m.getOriginalLanguage(), m.getTargetLanguage(),
                m.isRead(), m.getTimestamp());
    }
}
