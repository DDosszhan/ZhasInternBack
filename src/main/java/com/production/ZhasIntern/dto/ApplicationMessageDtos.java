package com.production.ZhasIntern.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class ApplicationMessageDtos {

    public record ChatMessageItem(
            UUID id,
            UUID applicationId,
            String senderId,
            String senderRole,
            String senderName,
            String body,
            Instant createdAt
    ) {}

    public record CreateChatMessageRequest(
            @NotBlank(message = "body is required")
            @Size(max = 4000, message = "body max length is 4000")
            String body
    ) {}
}