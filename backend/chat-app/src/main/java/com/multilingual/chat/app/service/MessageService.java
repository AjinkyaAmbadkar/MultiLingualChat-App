package com.multilingual.chat.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.multilingual.chat.app.dto.ConversationDto;
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
     * Phase 6: senderEmail derived from JWT Principal — client can no longer forge sender.
     * Phase 7: originalLanguage + targetLanguage derived from DB (sender/receiver preferredLanguage).
     *          OpenAI is called ONLY when the two languages differ — saves cost for same-language chats.
     *
     * @param requestDto  the message payload from the client (receiverId + text only)
     * @param senderEmail the authenticated sender's email — derived from JWT, never from the client
     */
    public MessageResponseDto sendMessage(SendMessageRequestDto requestDto, String senderEmail) {

        log.info("Sending message | sender email: {} → receiverId: {}",
                senderEmail, requestDto.getReceiverId());

        // Look up sender by email (from JWT) — cannot be forged by client
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderEmail));

        User receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + requestDto.getReceiverId()));

        // Derive languages from DB — client no longer supplies these
        // This ensures language preferences are always authoritative from the server side
        String senderLanguage   = sender.getPreferredLanguage();
        String receiverLanguage = receiver.getPreferredLanguage();

        // Only call OpenAI when languages actually differ — avoids unnecessary cost
        String translatedText;
        String senderTranslatedText;
        if (translationService.isTranslationRequired(senderLanguage, receiverLanguage)) {
            log.info("Translation required | {} → {}", senderLanguage, receiverLanguage);
            // Translate for receiver (what they will read)
            translatedText = translationService.translate(
                    requestDto.getOriginalText(),
                    senderLanguage,
                    receiverLanguage);
            // Translate to sender's preferred language (auto-detect source) so sender sees their own message correctly
            senderTranslatedText = translationService.translateToLanguage(
                    requestDto.getOriginalText(),
                    senderLanguage);
        } else {
            // Same language — deliver original text as-is, no API call
            log.info("No translation needed — both users speak: {}", senderLanguage);
            translatedText = requestDto.getOriginalText();
            senderTranslatedText = requestDto.getOriginalText();
        }

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOriginalLanguage(senderLanguage);
        message.setTargetLanguage(receiverLanguage);
        message.setOriginalText(requestDto.getOriginalText());
        message.setTranslatedText(translatedText);
        message.setSenderTranslatedText(senderTranslatedText);
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
                message.getSenderTranslatedText(),
                message.getOriginalLanguage(),
                message.getTargetLanguage(),
                message.getTimestamp());
    }

    public List<MessageResponseDto> getChatHistory(Long user1Id, Long user2Id) {
        return messageRepository.findChatHistory(user1Id, user2Id)
                .stream().map(this::mapToMessageResponseDto).toList();
    }

    /**
     * Returns the conversation list for the sidebar — one entry per unique chat partner,
     * showing the last message and the other person's info.
     *
     * Deduplication: the DB query may return two rows for the same partner (one where
     * current user was sender, one where receiver). We deduplicate by keeping only the
     * most recent row per partner using a LinkedHashMap keyed on partner userId.
     */
    public List<ConversationDto> getConversations(String currentUserEmail) {
        User me = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUserEmail));

        List<Message> latestMessages = messageRepository.findLatestMessagePerConversation(me);

        // Deduplicate: keep only the most recent message per conversation partner
        java.util.LinkedHashMap<Long, ConversationDto> byPartner = new java.util.LinkedHashMap<>();

        for (Message msg : latestMessages) {
            User partner = msg.getSender().getId().equals(me.getId())
                    ? msg.getReceiver()
                    : msg.getSender();

            if (byPartner.containsKey(partner.getId())) continue;

            // Show the translated text as preview if available (receiver sees translated)
            String preview = msg.getTranslatedText() != null
                    ? msg.getTranslatedText()
                    : msg.getOriginalText();

            byPartner.put(partner.getId(), new ConversationDto(
                    partner.getId(),
                    partner.getname(),
                    partner.getPictureUrl(),
                    preview,
                    msg.getTimestamp(),
                    0L   // unread count — placeholder until read receipts are implemented
            ));
        }

        return new java.util.ArrayList<>(byPartner.values());
    }

}
