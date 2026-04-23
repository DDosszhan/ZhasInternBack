package com.production.ZhasIntern.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class FileDtos {

    public record InitiateUploadRequest(
            @NotBlank(message = "fileName is required")
            String fileName,
            @NotBlank(message = "contentType is required")
            String contentType,
            @NotNull(message = "sizeBytes is required")
            @Positive(message = "sizeBytes must be positive")
            Long sizeBytes
    ) {}

    public record UploadTargetResponse(
            UUID fileId,
            String uploadUrl,
            String method,
            Map<String, String> headers,
            Instant expiresAt
    ) {}

    public record CompleteUploadResponse(
            UUID fileId,
            String status,
            String fileUrl
    ) {}

    public record DownloadUrlResponse(
            UUID fileId,
            String downloadUrl,
            Instant expiresAt
    ) {}

    public record AttachmentItem(
            UUID id,
            String originalFileName,
            String contentType,
            long sizeBytes,
            String url,
            Instant urlExpiresAt
    ) {}
}
