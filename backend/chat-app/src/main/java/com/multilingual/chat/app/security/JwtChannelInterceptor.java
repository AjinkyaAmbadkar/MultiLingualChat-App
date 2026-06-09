package com.multilingual.chat.app.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;

/**
 * STOMP channel interceptor — enforces JWT authentication on WebSocket connections.
 *
 * WHY we need this (and can't reuse JwtAuthFilter):
 * ────────────────────────────────────────────────────
 * JwtAuthFilter is an HTTP filter that runs on every HTTP request. WebSocket is different:
 * it starts as HTTP (the handshake/upgrade), then becomes a persistent TCP connection.
 * After the upgrade, JwtAuthFilter never runs again — HTTP filters don't apply to frames.
 *
 * STOMP has its own "login" moment: the CONNECT frame. This interceptor acts at that moment.
 * We validate the JWT, create an authenticated Principal, and attach it to the STOMP session.
 * Spring carries that Principal automatically for every subsequent frame on that connection.
 *
 * Flow:
 *   Client sends STOMP CONNECT:
 *     CONNECT
 *     Authorization: Bearer eyJhbGci...
 *     ^^^ (in STOMP native headers)
 *
 *   → preSend() fires
 *   → We parse the JWT, look up the user, build a UsernamePasswordAuthenticationToken
 *   → accessor.setUser(auth)  ← attaches Principal to the STOMP session
 *   → On success: message proceeds, Spring notes the Principal for this WebSocket session
 *   → On failure: we throw MessageDeliveryException → Spring sends STOMP ERROR + closes socket
 *
 *   Later, in ChatWebSocketController:
 *   → @MessageMapping method receives (Principal principal)
 *   → principal.getName() returns the email — server knows who sent the message
 *   → No more client-supplied senderId — the server owns identity
 */
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtChannelInterceptor.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtChannelInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Called before every STOMP frame is forwarded to the channel.
     * We only care about CONNECT frames — those are the authentication handshake.
     * SEND, SUBSCRIBE, DISCONNECT etc. inherit the Principal set at CONNECT time.
     *
     * @param message the inbound STOMP frame
     * @param channel the channel the message is about to be sent to
     * @return the (possibly mutated) message, or throw to reject the frame
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // accessor can be null for non-STOMP messages — let them pass through untouched
        if (accessor == null) {
            return message;
        }

        // Only authenticate on CONNECT frames — that's the WebSocket "login moment"
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        log.debug("[WS] STOMP CONNECT received — validating JWT");

        // ── Read the Authorization header ──────────────────────────────────────────
        // STOMP native headers are key-value pairs sent inside the CONNECT frame body,
        // distinct from HTTP headers. The client sends:
        //   stompClient.publish({ connectHeaders: { 'Authorization': 'Bearer <token>' } })
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[WS] CONNECT rejected — missing or malformed Authorization header");
            throw new MessageDeliveryException(message,
                    "WebSocket authentication failed: missing Authorization header. " +
                    "Include 'Authorization: Bearer <token>' in STOMP CONNECT headers.");
        }

        // ── Validate the JWT ───────────────────────────────────────────────────────
        String token = authHeader.substring(7); // strip "Bearer "

        try {
            String email = jwtService.extractEmail(token);

            if (email == null || !jwtService.isTokenValid(token, email)) {
                log.warn("[WS] CONNECT rejected — JWT invalid or expired for email: {}", email);
                throw new MessageDeliveryException(message,
                        "WebSocket authentication failed: invalid or expired JWT token.");
            }

            // ── Build the Spring Security Principal ────────────────────────────────
            // Load the full UserDetails (roles, authorities) so Spring Security is happy.
            // Then wrap in UsernamePasswordAuthenticationToken — this is a fully authenticated
            // token (3-arg constructor = authenticated, not just credentials presented).
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,                          // no credentials stored after authentication
                    userDetails.getAuthorities()   // roles (ROLE_USER)
            );

            // ── Attach the Principal to the STOMP session ──────────────────────────
            // This is the magic line. Spring stores this Principal in the WebSocket session
            // attributes. Every subsequent frame from this client carries the same Principal.
            // When ChatWebSocketController declares (Principal principal), Spring injects it here.
            accessor.setUser(auth);

            log.info("[WS] CONNECT authenticated — user: {}", email);

        } catch (JwtException e) {
            log.warn("[WS] CONNECT rejected — JWT parse error: {}", e.getMessage());
            throw new MessageDeliveryException(message,
                    "WebSocket authentication failed: " + e.getMessage());
        }

        return message;
    }
}
