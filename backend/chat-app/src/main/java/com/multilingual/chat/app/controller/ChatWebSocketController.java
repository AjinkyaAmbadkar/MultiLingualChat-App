package com.multilingual.chat.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.SendMessageRequestDto;
import com.multilingual.chat.app.service.MessageService;

/**
 * Handles real-time chat messages over WebSocket (STOMP protocol).
 *
 * Note: this is @Controller, NOT @RestController — WebSocket handlers
 * don't return HTTP responses; they push messages via SimpMessagingTemplate.
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;

    /**
     * SimpMessagingTemplate is Spring's way to PUSH messages from server → client.
     * "Simp" = Simple Messaging Protocol. This is auto-configured by @EnableWebSocketMessageBroker.
     * You inject it and call convertAndSend() to push to any topic destination.
     */
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
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
     * @Payload binds the incoming JSON body to SendMessageRequestDto
     * (same DTO as the REST endpoint — no duplication needed!)
     *
     * Flow:
     *   1. Receive message from sender
     *   2. Translate + save via MessageService (existing logic, unchanged)
     *   3. Push MessageResponseDto to receiver's topic → they see translated message
     *   4. Push MessageResponseDto back to sender's topic → they see their message confirmed/saved
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequestDto requestDto) {

        log.info("[WS] Message received | senderId: {} → receiverId: {}",
                requestDto.getSenderId(), requestDto.getReceiverId());

        // Reuse the exact same service that the REST endpoint uses.
        // This means: sender/receiver lookup, translation via OpenAI, save to DB.
        MessageResponseDto savedMessage = messageService.sendMessage(requestDto);

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
}
