package com.production.ZhasIntern.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ApplicationMessageDtos {

    public record ChatMessageItem(
            UUID id,
            UUID applicationId,
            String senderId,
            String senderRole,
            String senderName,
            String body,
            List<FileDtos.AttachmentItem> attachments,
            Instant createdAt
    ) {}

    public record CreateChatMessageRequest(
            String body,
            List<UUID> attachmentIds
    ) {}
}
