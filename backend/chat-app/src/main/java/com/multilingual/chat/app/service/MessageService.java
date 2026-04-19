package com.multilingual.chat.app.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.multilingual.chat.app.dto.MessageResponseDto;
import com.multilingual.chat.app.dto.SendMessageRequestDto;
import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.MessageRepository;
import com.multilingual.chat.app.repository.UserRepository;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public MessageResponseDto sendMessage(SendMessageRequestDto requestDto) {

        User sender = userRepository.findById(requestDto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found with " + requestDto.getSenderId()));

        User receiver = userRepository.findById(requestDto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("reciever not found with" + requestDto.getReceiverId()));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setOriginalLanguage(requestDto.getOriginalLanguage());
        message.setTargetLanguage(requestDto.getTargetLanguage());
        message.setOriginalText(requestDto.getOriginalText());
        message.setTranslatedText(requestDto.getTranslatedText());
        message.setTimestamp(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
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
