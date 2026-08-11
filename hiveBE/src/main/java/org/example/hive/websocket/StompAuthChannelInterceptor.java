package org.example.hive.websocket;

import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.security.CustomUserDetailsService;
import org.example.hive.security.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * HTTP requests get their JWT checked once per request by JwtAuthenticationFilter.
 * A STOMP/SockJS connection is one long-lived session instead, so the JWT is only
 * sent once, on the CONNECT frame (as an "Authorization" native header, same value
 * the REST API expects) - not on every message frame. We authenticate here and
 * stash the resulting principal on the STOMP session; every SEND/SUBSCRIBE that
 * follows on that session reuses it, the same way SecurityContextHolder is reused
 * for the rest of an HTTP filter chain.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Missing or malformed Authorization header on STOMP CONNECT");
            }

            String token = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(token);
            Long companyId = jwtService.extractCompanyId(token);

            UserDetails userDetails = userDetailsService.loadUser(userEmail, companyId);

            if (!jwtService.isTokenValid(token, userDetails) || !(userDetails instanceof AuthUserPrincipal principal)) {
                throw new IllegalArgumentException("Invalid or expired token on STOMP CONNECT");
            }

            // Everything downstream (the @MessageMapping controller, and any
            // future SecurityConfig rule on the message channel) reads this.
            accessor.setUser(principal);
        }

        return message;
    }
}