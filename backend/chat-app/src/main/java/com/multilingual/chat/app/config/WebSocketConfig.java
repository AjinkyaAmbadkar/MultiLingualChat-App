package com.multilingual.chat.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures WebSocket messaging with STOMP protocol.
 *
 * Two key things to configure:
 *   1. The WebSocket endpoint clients connect to (the "door")
 *   2. The message broker that routes messages between clients (the "post office")
 */
@Configuration
@EnableWebSocketMessageBroker  // turns on the full STOMP-over-WebSocket stack
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers the WebSocket handshake endpoint.
     * This is the URL clients use to UPGRADE from HTTP to WebSocket.
     *
     * .withSockJS() adds a fallback: if the browser doesn't support native
     * WebSocket, SockJS silently falls back to HTTP long-polling.
     * Your app code doesn't need to change — SockJS handles it transparently.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")           // clients connect to ws://localhost:8080/ws
                .setAllowedOriginPatterns("*") // allow all origins (tighten this in prod)
                .withSockJS();                 // SockJS fallback for non-WebSocket browsers
    }

    /**
     * Configures the in-memory message broker — the routing rules for STOMP messages.
     *
     * Think of this as defining two lanes of traffic:
     *
     *   /app  → incoming lane: messages FROM clients TO your @MessageMapping methods
     *           e.g. client sends to "/app/chat.send" → routed to @MessageMapping("/chat.send")
     *
     *   /topic → outgoing lane: messages FROM server TO subscribed clients
     *            e.g. server sends to "/topic/user.2" → delivered to all clients subscribed there
     *
     * enableSimpleBroker() uses an in-memory broker (perfect for development).
     * Later you can swap it for a real broker like RabbitMQ or Redis for production scale.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app"); // prefix for @MessageMapping routes
        config.enableSimpleBroker("/topic");               // prefix for server → client pushes
    }
}
