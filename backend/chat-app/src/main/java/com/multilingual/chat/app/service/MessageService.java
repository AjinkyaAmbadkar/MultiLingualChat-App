package com.multilingual.chat.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.SendMessageRequestDto;
import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.MessageRepository;
import com.multilingual.chat.app.repository.UserRepository;
@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TranslationService translationService;

    // Constructor injection — Spring automatically wires all three dependencies.
    // Since OpenAiTranslationServiceImpl is @Primary, that's what gets injected here.
    public MessageService(MessageRepository messageRepository,
            UserRepository userRepository,
            TranslationService translationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.translationService = translationService;
    }

    /**
     * Core message-sending logic used by both the WebSocket and REST paths.
     *
     * Phase 6 change: senderEmail is now a separate parameter — the caller (controller)
     * passes in the authenticated user's email from the JWT Principal.
     * senderId is no longer read from the DTO (it was removed to close an impersonation hole).
     *
     * @param requestDto  the message payload from the client (receiverId, text, languages)
     * @param senderEmail the authenticated sender's email — derived from JWT, never from the client
     */
    public MessageResponseDto sendMessage(SendMessageRequestDto requestDto, String senderEmail) {

        log.info("Sending message | sender email: {} → receiverId: {}",
                senderEmail, requestDto.getReceiverId());

        // Look up sender by email (from the JWT) — the client can no longer supply this
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderEmail));

        User receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + requestDto.getReceiverId()));

        // Server-side translation — the client no longer sends translatedText.
        // isTranslationRequired() skips the API call when source == target language.
        String translatedText;
        if (translationService.isTranslationRequired(requestDto.getOriginalLanguage(),
                requestDto.getTargetLanguage())) {
            log.info("Translation required | {} → {}", requestDto.getOriginalLanguage(),
                    requestDto.getTargetLanguage());
            translatedText = translationService.translate(
                    requestDto.getOriginalText(),
                    requestDto.getOriginalLanguage(),
                    requestDto.getTargetLanguage());
        } else {
            // Same language — no translation needed, store original text as-is
            log.info("No translation needed — source and target language are the same: {}",
                    requestDto.getOriginalLanguage());
            translatedText = requestDto.getOriginalText();
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOriginalLanguage(requestDto.getOriginalLanguage());
        message.setTargetLanguage(requestDto.getTargetLanguage());
        message.setOriginalText(requestDto.getOriginalText());
        message.setTranslatedText(translatedText);
        message.setTimestamp(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved successfully | messageId: {}", savedMessage.getId());
        return mapToMessageResponseDto(savedMessage);
    }

    private MessageResponseDto mapToMessageResponseDto(Message message) {
        return new MessageResponseDto(
                message.getId(),
                message.getSender().getId(),
                // message.getSender().getName(),
                message.getReceiver().getId(),
                // message.getReceiver().getName(),
                message.getOriginalText(),
                message.getTranslatedText(),
                message.getOriginalLanguage(),
                message.getTargetLanguage(),
                message.getTimestamp());
    }

    public List<Message> getChatHistory(Long user1Id, Long user2Id) {
        return messageRepository.findChatHistory(user1Id, user2Id);
    }

}
