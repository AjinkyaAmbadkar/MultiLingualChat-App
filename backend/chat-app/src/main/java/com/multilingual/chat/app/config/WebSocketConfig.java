package com.multilingual.chat.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.multilingual.chat.app.security.JwtChannelInterceptor;

/**
 * Configures WebSocket messaging with STOMP protocol.
 *
 * Three things configured here (Phase 6 adds the third):
 * 1. The WebSocket endpoint clients connect to (the "door")
 * 2. The message broker that routes messages between clients (the "post
 * office")
 * 3. A channel interceptor that validates JWT on every STOMP CONNECT frame (the
 * "security guard")
 */
@Configuration
@EnableWebSocketMessageBroker // turns on the full STOMP-over-WebSocket stack
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Phase 6: the interceptor that checks JWT on every STOMP CONNECT frame.
    //
    // WHY @Lazy here?
    // ───────────────
    // @EnableWebSocketMessageBroker causes Spring to build the STOMP infrastructure
    // early in the context lifecycle — before JPA's entity manager factory is
    // ready.
    // Without @Lazy, this constructor forces Spring to create JwtChannelInterceptor
    // immediately, which pulls in UserDetailsServiceImpl → UserRepository → JPA,
    // causing a "Cannot resolve jpaSharedEM_entityManagerFactory" startup failure.
    //
    // @Lazy creates a lightweight proxy here instead of the real bean. The real
    // JwtChannelInterceptor is only instantiated the first time it's actually
    // called
    // (i.e. when the first WebSocket CONNECT frame arrives), by which point JPA
    // and all other infrastructure are fully initialized.
    private final JwtChannelInterceptor jwtChannelInterceptor;

    public WebSocketConfig(@Lazy JwtChannelInterceptor jwtChannelInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
    }

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
        registry.addEndpoint("/ws") // clients connect to ws://localhost:8080/ws
                .setAllowedOriginPatterns("*") // allow all origins (tighten this in prod)
                .withSockJS(); // SockJS fallback for non-WebSocket browsers
    }

    /**
     * Configures the in-memory message broker — the routing rules for STOMP
     * messages.
     *
     * Think of this as defining two lanes of traffic:
     *
     * /app → incoming lane: messages FROM clients TO your @MessageMapping methods
     * e.g. client sends to "/app/chat.send" → routed
     * to @MessageMapping("/chat.send")
     *
     * /topic → outgoing lane: messages FROM server TO subscribed clients
     * e.g. server sends to "/topic/user.2" → delivered to all clients subscribed
     * there
     *
     * enableSimpleBroker() uses an in-memory broker (perfect for development).
     * Later you can swap it for a real broker like RabbitMQ or Redis for production
     * scale.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app"); // prefix for @MessageMapping routes
        config.enableSimpleBroker("/topic"); // prefix for server → client pushes
    }

    /**
     * Phase 6: Register JwtChannelInterceptor on the inbound channel.
     *
     * The "client inbound channel" carries all STOMP frames FROM clients TO the
     * server.
     * Our interceptor fires on every frame before it reaches any @MessageMapping
     * handler.
     *
     * It only acts on CONNECT frames (validates the JWT, sets the Principal).
     * SEND / SUBSCRIBE / DISCONNECT frames pass through — they inherit the
     * Principal that
     * was set at CONNECT time and is stored in the WebSocket session.
     *
     * Why configureClientInboundChannel and not configureClientOutboundChannel?
     * inbound = frames the CLIENT sends TO the server → we guard the entrance here
     * outbound = frames the SERVER sends TO the client → nothing to authenticate
     * there
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
