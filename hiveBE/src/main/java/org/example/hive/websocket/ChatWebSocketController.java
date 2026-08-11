package org.example.hive.websocket;

import jakarta.validation.Valid;
import org.example.hive.dto.request.SendMessageRequest;
import org.example.hive.security.AuthUserPrincipal;
import org.example.hive.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * The REST endpoint (MessageController) and this class are two doors into the
 * exact same MessageService.send(...) - membership checks, persistence, and
 * the broadcast all happen once in the service. This controller's only job is
 * to unwrap the STOMP frame into the same (conversationId, senderId, companyId,
 * request) shape the REST controller already builds.
 *
 * Client sends to:      /app/conversations/{conversationId}/send
 * Client subscribes to: /topic/conversations/{conversationId}
 */
@Controller
public class ChatWebSocketController {

    private final MessageService messageService;

    public ChatWebSocketController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/conversations/{conversationId}/send")
    public void send(@DestinationVariable Long conversationId,
                     @Valid @Payload SendMessageRequest request,
                     Principal principal) {
        // StompAuthChannelInterceptor guarantees this is an AuthUserPrincipal -
        // any frame that reached here already passed CONNECT-time JWT auth.
        AuthUserPrincipal user = (AuthUserPrincipal) principal;
        messageService.send(conversationId, user.getUserId(), user.getCompanyId(), request);
        // No return value: the service already published the MessageResponse
        // to /topic/conversations/{conversationId}, which is what every
        // subscribed member (sender included) is listening on.
    }
}