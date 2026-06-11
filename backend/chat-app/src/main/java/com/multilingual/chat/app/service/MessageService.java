package com.multilingual.chat.app.service;

import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
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
    private final EncryptionService encryptionService;

    public MessageService(MessageRepository messageRepository,
            UserRepository userRepository,
            TranslationService translationService,
            EncryptionService encryptionService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.translationService = translationService;
        this.encryptionService = encryptionService;
    }

    /**
     * Core message-sending logic.
     *
     * Phase 8.5: after translation, both plaintext strings are AES-256-GCM encrypted
     * before being persisted. The AES key is wrapped with each user's RSA public key.
     * Plaintext variables are nulled out after use (best-effort; GC handles the rest).
     */
    public MessageResponseDto sendMessage(SendMessageRequestDto requestDto, String senderEmail) {

        log.info("Sending message | sender email: {} → receiverId: {}",
                senderEmail, requestDto.getReceiverId());

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderEmail));

        User receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + requestDto.getReceiverId()));

        String senderLanguage   = sender.getPreferredLanguage();
        String receiverLanguage = receiver.getPreferredLanguage();

        String originalText;
        String translatedText;
        String senderTranslatedText;

        originalText = requestDto.getOriginalText();
        // Sender always sees what they typed — no need to call OpenAI for senderTranslatedText
        senderTranslatedText = originalText;

        if (translationService.isTranslationRequired(senderLanguage, receiverLanguage)) {
            log.info("Translation required | {} → {}", senderLanguage, receiverLanguage);
            translatedText = translationService.translate(originalText, senderLanguage, receiverLanguage);
        } else {
            log.info("No translation needed — both users speak: {}", senderLanguage);
            translatedText = originalText;
        }

        // ── Encrypt all three plaintext variants ─────────────────────────────
        if (sender.getPublicKey() == null) {
            throw new RuntimeException(
                "Sender has no encryption keypair. Please log out and log back in to generate one.");
        }
        if (receiver.getPublicKey() == null) {
            throw new RuntimeException(
                "Receiver has no encryption keypair. They must log in once before you can message them.");
        }
        PublicKey senderPublicKey   = encryptionService.decodePublicKey(sender.getPublicKey());
        PublicKey receiverPublicKey = encryptionService.decodePublicKey(receiver.getPublicKey());

        // Fresh AES key per message; separate IV per plaintext field (GCM nonce uniqueness)
        byte[] aesKey           = encryptionService.generateAesKey();
        byte[] ivOriginal       = encryptionService.generateAesIv();
        byte[] ivTranslated     = encryptionService.generateAesIv();
        byte[] ivSender         = encryptionService.generateAesIv();

        String encOriginal      = encryptionService.aesEncrypt(aesKey, ivOriginal,    originalText);
        String encTranslated    = encryptionService.aesEncrypt(aesKey, ivTranslated,  translatedText);
        String encSender        = encryptionService.aesEncrypt(aesKey, ivSender,      senderTranslatedText);

        String wrappedForSender   = encryptionService.rsaEncrypt(senderPublicKey,   aesKey);
        String wrappedForReceiver = encryptionService.rsaEncrypt(receiverPublicKey, aesKey);

        // Best-effort plaintext zeroing
        originalText        = null;
        translatedText      = null;
        senderTranslatedText = null;

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOriginalLanguage(senderLanguage);
        message.setTargetLanguage(receiverLanguage);
        message.setEncryptedOriginalText(encOriginal);
        message.setEncryptedTranslatedText(encTranslated);
        message.setEncryptedSenderText(encSender);
        message.setAesKeyForSender(wrappedForSender);
        message.setAesKeyForReceiver(wrappedForReceiver);
        message.setAesIvOriginal(Base64.getEncoder().encodeToString(ivOriginal));
        message.setAesIvTranslated(Base64.getEncoder().encodeToString(ivTranslated));
        message.setAesIvSender(Base64.getEncoder().encodeToString(ivSender));
        message.setTimestamp(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved (encrypted) | messageId: {}", savedMessage.getId());
        return mapToDto(savedMessage);
    }

    private MessageResponseDto mapToDto(Message message) {
        return new MessageResponseDto(
                message.getId(),
                message.getSender().getId(),
                message.getReceiver().getId(),
                message.getEncryptedOriginalText(),
                message.getEncryptedTranslatedText(),
                message.getEncryptedSenderText(),
                message.getAesKeyForSender(),
                message.getAesKeyForReceiver(),
                message.getAesIvOriginal(),
                message.getAesIvTranslated(),
                message.getAesIvSender(),
                message.getOriginalLanguage(),
                message.getTargetLanguage(),
                message.isRead(),
                message.getTimestamp());
    }

    public List<MessageResponseDto> getChatHistory(Long user1Id, Long user2Id) {
        return messageRepository.findChatHistory(user1Id, user2Id)
                .stream().map(this::mapToDto).toList();
    }

    /**
     * Returns the conversation list for the sidebar — one entry per unique chat partner.
     * Preview text is omitted (ciphertext is meaningless); timestamp is shown instead.
     */
    public List<ConversationDto> getConversations(String currentUserEmail) {
        User me = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUserEmail));

        List<Message> latestMessages = messageRepository.findLatestMessagePerConversation(me);

        java.util.LinkedHashMap<Long, ConversationDto> byPartner = new java.util.LinkedHashMap<>();

        for (Message msg : latestMessages) {
            User partner = msg.getSender().getId().equals(me.getId())
                    ? msg.getReceiver()
                    : msg.getSender();

            if (byPartner.containsKey(partner.getId())) continue;

            // Preview is unavailable server-side (ciphertext); client should decrypt and display.
            byPartner.put(partner.getId(), new ConversationDto(
                    partner.getId(),
                    partner.getname(),
                    partner.getPictureUrl(),
                    null,   // client decrypts preview from the encrypted blob
                    msg.getTimestamp(),
                    0L
            ));
        }

        return new java.util.ArrayList<>(byPartner.values());
    }
}
