package com.multilingual.chat.app.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.multilingual.chat.app.dto.PresenceEventDto;
import com.multilingual.chat.app.entity.User;
import com.multilingual.chat.app.repository.MessageRepository;
import com.multilingual.chat.app.repository.UserRepository;
import com.multilingual.chat.app.service.PresenceService;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PresenceService presenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public WebSocketEventListener(PresenceService presenceService,
                                  SimpMessagingTemplate messagingTemplate,
                                  UserRepository userRepository,
                                  MessageRepository messageRepository) {
        this.presenceService    = presenceService;
        this.messagingTemplate  = messagingTemplate;
        this.userRepository     = userRepository;
        this.messageRepository  = messageRepository;
    }

    @EventListener
    @Transactional
    public void handleConnect(SessionConnectedEvent event) {
        // Principal lives on the event, not the CONNECTED frame headers
        if (event.getUser() == null) return;
        String email = event.getUser().getName();

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        userRepository.findByEmail(email).ifPresent(user -> {
            presenceService.connect(sessionId, user.getId());
            log.info("[Presence] User {} ({}) connected — session {}", email, user.getId(), sessionId);

            broadcastPresence(user, true);
            notifyUserOfContactStatuses(user);
        });
    }

    @EventListener
    @Transactional
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();

        presenceService.disconnect(sessionId).ifPresent(userId -> {
            userRepository.findById(userId).ifPresent(user -> {
                log.info("[Presence] User {} ({}) disconnected — session {}", user.getemail(), userId, sessionId);
                broadcastPresence(user, false);
            });
        });
    }

    // Push a PRESENCE event to every conversation partner of the given user
    private void broadcastPresence(User user, boolean online) {
        List<User> partners = getConversationPartners(user);
        log.info("[Presence] Broadcasting {} for user {} to {} partners", online ? "ONLINE" : "OFFLINE", user.getId(), partners.size());
        PresenceEventDto event = new PresenceEventDto(user.getId(), online);
        partners.forEach(partner -> {
            String topic = "/topic/user." + partner.getId();
            messagingTemplate.convertAndSend(topic, event);
            log.info("[Presence] Pushed {} status to partner {} at {}", online ? "ONLINE" : "OFFLINE", partner.getId(), topic);
        });
    }

    // On connect: tell the newly connected user which of their contacts are already online
    private void notifyUserOfContactStatuses(User user) {
        String myTopic = "/topic/user." + user.getId();
        List<User> partners = getConversationPartners(user);
        log.info("[Presence] Notifying user {} of {} contacts statuses", user.getId(), partners.size());
        partners.forEach(partner -> {
            boolean online = presenceService.isOnline(partner.getId());
            log.info("[Presence] Contact {} is {}", partner.getId(), online ? "ONLINE" : "OFFLINE");
            messagingTemplate.convertAndSend(myTopic, new PresenceEventDto(partner.getId(), online));
        });
    }

    private List<User> getConversationPartners(User user) {
        List<Long> partnerIds = messageRepository.findConversationPartnerIds(user.getId());
        log.info("[Presence] Found {} conversation partner IDs for user {}: {}", partnerIds.size(), user.getId(), partnerIds);
        return userRepository.findAllById(partnerIds);
    }
}
