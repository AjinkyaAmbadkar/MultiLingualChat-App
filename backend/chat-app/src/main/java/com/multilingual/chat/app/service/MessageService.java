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
import com.multilingual.chat.app.dto.TranslationJobDto;
import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.entity.MessageStatus;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.kafka.TranslationProducer;
import com.multilingual.chat.app.repository.MessageRepository;
import com.multilingual.chat.app.repository.UserRepository;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final UserLanguageCacheService languageCache;
    private final TranslationProducer translationProducer;

    public MessageService(MessageRepository messageRepository,
            UserRepository userRepository,
            EncryptionService encryptionService,
            UserLanguageCacheService languageCache,
            TranslationProducer translationProducer) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.languageCache = languageCache;
        this.translationProducer = translationProducer;
    }

    /**
     * Quick-save: encrypts and persists the message immediately (status=PENDING),
     * then publishes a translation job to Kafka.
     *
     * The sender gets an instant ACK. The Kafka consumer handles OpenAI translation
     * and delivers the final message to the receiver asynchronously.
     */
    public MessageResponseDto sendMessage(SendMessageRequestDto requestDto, String senderEmail) {
        log.info("Quick-save | sender: {} → receiverId: {}", senderEmail, requestDto.getReceiverId());

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderEmail));
        User receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found: " + requestDto.getReceiverId()));

        if (sender.getPublicKey() == null)
            throw new RuntimeException("Sender has no keypair — log out and back in.");
        if (receiver.getPublicKey() == null)
            throw new RuntimeException("Receiver has no keypair — they must log in once first.");

        String originalText    = requestDto.getOriginalText();
        String senderLanguage  = languageCache.getPreferredLanguage(sender.getId());
        String receiverLanguage = languageCache.getPreferredLanguage(receiver.getId());

        PublicKey senderPk   = encryptionService.decodePublicKey(sender.getPublicKey());
        PublicKey receiverPk = encryptionService.decodePublicKey(receiver.getPublicKey());

        // Encrypt originalText now; translated fields will be filled by the Kafka consumer
        byte[] aesKey     = encryptionService.generateAesKey();
        byte[] ivOriginal = encryptionService.generateAesIv();

        String encOriginal       = encryptionService.aesEncrypt(aesKey, ivOriginal, originalText);
        String wrappedForSender  = encryptionService.rsaEncrypt(senderPk, aesKey);
        String wrappedForReceiver = encryptionService.rsaEncrypt(receiverPk, aesKey);

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOriginalLanguage(senderLanguage);
        message.setTargetLanguage(receiverLanguage);
        message.setEncryptedOriginalText(encOriginal);
        message.setEncryptedTranslatedText(encOriginal);  // placeholder until consumer updates it
        message.setEncryptedSenderText(encOriginal);       // placeholder until consumer updates it
        message.setAesKeyForSender(wrappedForSender);
        message.setAesKeyForReceiver(wrappedForReceiver);
        message.setAesIvOriginal(Base64.getEncoder().encodeToString(ivOriginal));
        message.setAesIvTranslated(Base64.getEncoder().encodeToString(ivOriginal)); // same IV for placeholder
        message.setAesIvSender(Base64.getEncoder().encodeToString(ivOriginal));     // same IV for placeholder
        message.setTimestamp(LocalDateTime.now(java.time.ZoneOffset.UTC)); // store UTC so clients can localize
        message.setStatus(MessageStatus.PENDING);

        Message saved = messageRepository.save(message);
        log.info("Message quick-saved (PENDING) | messageId: {}", saved.getId());

        // Publish translation job to Kafka — consumer will translate and push to receiver
        translationProducer.publishTranslationJob(new TranslationJobDto(
                saved.getId(), sender.getId(), receiver.getId(),
                originalText, senderLanguage, receiverLanguage,
                sender.getPublicKey(), receiver.getPublicKey()));

        return mapToDto(saved);
    }

    private MessageResponseDto mapToDto(Message message) {
        return new MessageResponseDto(
                message.getId(), message.getSender().getId(), message.getReceiver().getId(),
                message.getEncryptedOriginalText(), message.getEncryptedTranslatedText(),
                message.getEncryptedSenderText(), message.getAesKeyForSender(),
                message.getAesKeyForReceiver(), message.getAesIvOriginal(),
                message.getAesIvTranslated(), message.getAesIvSender(),
                message.getOriginalLanguage(), message.getTargetLanguage(),
                message.isRead(), message.getTimestamp());
    }

    public List<MessageResponseDto> getChatHistory(Long user1Id, Long user2Id) {
        return messageRepository.findChatHistory(user1Id, user2Id)
                .stream().map(this::mapToDto).toList();
    }

    public List<ConversationDto> getConversations(String currentUserEmail) {
        User me = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUserEmail));

        List<Message> latestMessages = messageRepository.findLatestMessagePerConversation(me);
        java.util.LinkedHashMap<Long, ConversationDto> byPartner = new java.util.LinkedHashMap<>();

        for (Message msg : latestMessages) {
            User partner = msg.getSender().getId().equals(me.getId())
                    ? msg.getReceiver() : msg.getSender();
            if (byPartner.containsKey(partner.getId())) continue;
            byPartner.put(partner.getId(), new ConversationDto(
                    partner.getId(), partner.getname(), partner.getPictureUrl(),
                    null, msg.getTimestamp(), 0L));
        }

        return new java.util.ArrayList<>(byPartner.values());
    }
}
