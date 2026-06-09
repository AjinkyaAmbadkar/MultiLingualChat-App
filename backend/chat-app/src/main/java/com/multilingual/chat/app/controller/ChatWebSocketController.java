package com.multilingual.chat.app.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.SendMessageRequestDto;
import com.multilingual.chat.app.dto.TypingEventDto;
import com.multilingual.chat.app.dto.TypingRequestDto;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.UserRepository;
import com.multilingual.chat.app.service.MessageService;

/**
 * Handles real-time chat messages over WebSocket (STOMP protocol).
 *
 * Note: this is @Controller, NOT @RestController — WebSocket handlers
 * don't return HTTP responses; they push messages via SimpMessagingTemplate.
 *
 * Phase 6 change: sendMessage() now takes a Principal parameter.
 * Spring injects the Principal that JwtChannelInterceptor set during the STOMP CONNECT.
 * We extract the sender's email from it — no more client-supplied senderId in the payload.
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;
    private final UserRepository userRepository;

    /**
     * SimpMessagingTemplate is Spring's way to PUSH messages from server → client.
     * "Simp" = Simple Messaging Protocol. This is auto-configured by @EnableWebSocketMessageBroker.
     * You inject it and call convertAndSend() to push to any topic destination.
     */
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   UserRepository userRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handles a chat message sent by a client over WebSocket.
     *
     * @MessageMapping("/chat.send") means:
     *   - Client sends a STOMP message to destination "/app/chat.send"
     *   - ("/app" prefix is stripped by the broker config, leaving "/chat.send")
     *   - Spring routes it to this method
     *
     * @Payload  — binds the incoming JSON body to SendMessageRequestDto
     * Principal — injected by Spring from the STOMP session. It was set by
     *             JwtChannelInterceptor.preSend() when the client sent CONNECT.
     *             principal.getName() returns the email (the JWT "subject" claim).
     *
     * Flow:
     *   1. Extract sender's email from Principal (JWT) — NOT from the client payload
     *   2. Pass to MessageService which looks up the User by email
     *   3. Translate + save via MessageService (sender/receiver lookup, OpenAI, DB save)
     *   4. Push MessageResponseDto to receiver's topic → they see translated message
     *   5. Echo back to sender's topic → they get the server-assigned ID + timestamp
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequestDto requestDto, Principal principal) {

        // principal.getName() → AbstractAuthenticationToken.getName() → UserDetails.getUsername()
        // = the email, because JwtChannelInterceptor wrapped UserDetails in the Authentication
        String senderEmail = principal.getName();

        log.info("[WS] Message received | sender: {} → receiverId: {}",
                senderEmail, requestDto.getReceiverId());

        // The service now receives the senderEmail from the JWT — not from the DTO.
        // It looks up the User by email, so the sender cannot be forged.
        MessageResponseDto savedMessage = messageService.sendMessage(requestDto, senderEmail);

        // Push the translated message to the RECEIVER.
        // They're subscribed to "/topic/user.{theirId}" and will receive it instantly.
        String receiverTopic = "/topic/user." + savedMessage.getReceiverId();
        messagingTemplate.convertAndSend(receiverTopic, savedMessage);
        log.info("[WS] Message pushed to receiver topic: {}", receiverTopic);

        // Echo the saved message back to the SENDER too.
        // Why? So the sender's UI gets the server-assigned message ID and timestamp
        // (important for ordering and deduplication in a real chat UI).
        String senderTopic = "/topic/user." + savedMessage.getSenderId();
        messagingTemplate.convertAndSend(senderTopic, savedMessage);
        log.info("[WS] Message echoed back to sender topic: {}", senderTopic);
    }

    /**
     * Relays a typing indicator from sender → receiver.
     * No DB interaction — purely in-memory relay. The receiver's UI shows/hides
     * the typing bubble based on the isTyping flag.
     */
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequestDto requestDto, Principal principal) {
        String senderEmail = principal.getName();

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found: " + senderEmail));

        TypingEventDto event = new TypingEventDto(sender.getId(), requestDto.getReceiverId(), requestDto.isTyping());

        String receiverTopic = "/topic/user." + requestDto.getReceiverId();
        messagingTemplate.convertAndSend(receiverTopic, event);
        log.debug("[WS] Typing event relayed | sender: {} → receiver topic: {}", senderEmail, receiverTopic);
    }
}
