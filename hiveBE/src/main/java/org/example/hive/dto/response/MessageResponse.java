package org.example.hive.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.hive.config.AppEnums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderUserId;
    private String senderName;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;
    private List<AttachmentResponse> attachments;
}