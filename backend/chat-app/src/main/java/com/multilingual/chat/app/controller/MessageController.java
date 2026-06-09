package com.multilingual.chat.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.multilingual.chat.app.dto.ConversationDto;
import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.SendMessageRequestDto;
import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.service.MessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * REST endpoint for sending a message.
     *
     * Phase 6 change: senderId is no longer read from the request body.
     * The Authentication object is populated by JwtAuthFilter (runs on every HTTP request).
     * authentication.getName() returns the email from the JWT subject claim.
     *
     * This mirrors the WebSocket path — the server always derives who the sender is
     * from the authenticated Principal, never from the client payload.
     */
    @PostMapping("/send")
    public MessageResponseDto sendMessage(@Valid @RequestBody SendMessageRequestDto requestDto,
                                          Authentication authentication) {
        String senderEmail = authentication.getName();
        log.debug("REST /send | sender: {} | receiverId: {}", senderEmail, requestDto.getReceiverId());
        return messageService.sendMessage(requestDto, senderEmail);
    }

    @GetMapping("/history")
    public List<MessageResponseDto> getChatHistory(@RequestParam long user1Id,
            @RequestParam long user2Id) {
        return messageService.getChatHistory(user1Id, user2Id);
    }

    /**
     * Returns the conversation list for the authenticated user's sidebar.
     * One entry per unique chat partner, ordered by most recent message first.
     */
    @GetMapping("/conversations")
    public List<ConversationDto> getConversations(Authentication authentication) {
        log.debug("GET /api/messages/conversations | user: {}", authentication.getName());
        return messageService.getConversations(authentication.getName());
    }

}
