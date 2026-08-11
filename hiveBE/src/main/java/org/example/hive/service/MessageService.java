package org.example.hive.service;

import org.example.hive.dto.request.SendMessageRequest;
import org.example.hive.dto.response.MessageResponse;
import org.example.hive.exception.ConversationException;
import org.example.hive.mapper.MessageMapper;
import org.example.hive.model.Conversation;
import org.example.hive.model.Message;
import org.example.hive.model.User;
import org.example.hive.repository.AttachmentRepository;
import org.example.hive.repository.ConversationMemberRepository;
import org.example.hive.repository.MessageRepository;
import org.example.hive.repository.UserRepository;
import org.example.hive.security.Permissions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          ConversationMemberRepository conversationMemberRepository,
                          AttachmentRepository attachmentRepository,
                          UserRepository userRepository,
                          ConversationService conversationService,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageResponse send(Long conversationId, Long senderId, Long companyId, SendMessageRequest req) {
        // Membership enforcement lives in ConversationService so it's identical
        // for every entry point (REST here, and ChatWebSocketController).
        Conversation conversation = conversationService.requireMemberConversation(conversationId, senderId, companyId);
        User sender = findUser(senderId);

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(req.getContent())
                .messageType(req.getMessageType())
                .build());

        // Sending implies you've read up to that point, so the sender's own
        // unread count never includes the message they just sent.
        markReadInternal(conversationId, senderId);

        MessageResponse response = MessageMapper.toResponse(message, List.of());

        // Broadcast regardless of entry point (REST here, or the STOMP
        // controller calling this same method) so every connected member's
        // client updates live, not just the sender's.
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> list(Long conversationId, Long requesterId, Long companyId, Pageable pageable) {
        conversationService.requireMemberConversation(conversationId, requesterId, companyId);
        return messageRepository.findAllByConversation_IdOrderByCreatedAtDesc(conversationId, pageable)
                .map(m -> MessageMapper.toResponse(m, attachmentRepository.findAllByMessage_IdOrderByIdAsc(m.getId())));
    }

    @Transactional
    public void delete(Long conversationId, Long messageId, Long requesterId, Long companyId) {
        conversationService.requireMemberConversation(conversationId, requesterId, companyId);

        Message message = messageRepository.findByIdAndConversation_Id(messageId, conversationId)
                .orElseThrow(() -> new ConversationException("Message not found", HttpStatus.NOT_FOUND));

        boolean isSender = message.getSender().getId().equals(requesterId);
        if (!isSender && !hasOverride(Permissions.MESSAGE_DELETE)) {
            throw new ConversationException("Only the sender or a manager can delete this message", HttpStatus.FORBIDDEN);
        }

        // Attachments own the FK back to the message (no ON DELETE CASCADE on
        // message_id — same shape as post_id/comment_id), so they must go first.
        attachmentRepository.deleteAll(attachmentRepository.findAllByMessage_IdOrderByIdAsc(messageId));
        messageRepository.delete(message);

        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/deletions", messageId);
    }

    private void markReadInternal(Long conversationId, Long userId) {
        conversationMemberRepository.findByConversation_IdAndUser_Id(conversationId, userId)
                .ifPresent(member -> {
                    member.setLastReadAt(LocalDateTime.now());
                    conversationMemberRepository.save(member);
                });
    }

    private boolean hasOverride(String permission) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(permission::equals);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ConversationException("User not found", HttpStatus.NOT_FOUND));
    }
}