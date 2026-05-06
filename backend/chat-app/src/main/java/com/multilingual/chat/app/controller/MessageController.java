package com.multilingual.chat.app.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.SendMessageRequestDto;
import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.service.MessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public MessageResponseDto sendMessage(@Valid @RequestBody SendMessageRequestDto requestDto) {
        log.debug("Message body at controller {}", requestDto);
        return messageService.sendMessage(requestDto);

    }

    @GetMapping("/history")
    public List<Message> getChatHistory(@RequestParam long user1Id,
            @RequestParam long user2Id) {
        return messageService.getChatHistory(user1Id, user2Id);
    }

}
