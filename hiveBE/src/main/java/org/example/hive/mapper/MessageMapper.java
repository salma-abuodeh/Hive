package org.example.hive.mapper;

import org.example.hive.dto.response.AttachmentResponse;
import org.example.hive.dto.response.MessageResponse;
import org.example.hive.model.Attachment;
import org.example.hive.model.Message;

import java.util.List;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message, List<Attachment> attachments) {
        List<AttachmentResponse> attachmentResponses = attachments.stream()
                .map(a -> new AttachmentResponse(
                        a.getId(),
                        "/attachments/" + a.getId(),
                        a.getContentType(),
                        a.getSizeBytes(),
                        a.getAttachmentType()))
                .toList();

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getFirstName() + " " + message.getSender().getLastName(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt(),
                attachmentResponses
        );
    }
}