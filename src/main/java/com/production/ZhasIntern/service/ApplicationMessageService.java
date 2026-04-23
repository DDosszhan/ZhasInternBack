package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.ApplicationMessageDtos;
import com.production.ZhasIntern.dto.FileDtos;
import com.production.ZhasIntern.entity.ApplicationMessage;
import com.production.ZhasIntern.entity.MessageAttachment;
import com.production.ZhasIntern.entity.StoredFile;
import com.production.ZhasIntern.entity.StoredFilePurpose;
import com.production.ZhasIntern.entity.StoredFileStatus;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.MessageAttachmentRepository;
import com.production.ZhasIntern.repository.ApplicationMessageRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationMessageService {

    private final ApplicationMessageRepository applicationMessageRepository;
    private final ProfileRepository profileRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final StoredFileService storedFileService;
    private final ApplicationAccessService applicationAccessService;

    public List<ApplicationMessageDtos.ChatMessageItem> listMessages(UUID applicationId, String currentUserId) {
        applicationAccessService.ensureCanReadChat(applicationId, currentUserId);

        List<ApplicationMessage> messages = applicationMessageRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId);
        return buildChatItems(messages);
    }

    @Transactional
    public ApplicationMessageDtos.ChatMessageItem createMessage(
            UUID applicationId,
            String currentUserId,
            ApplicationMessageDtos.CreateChatMessageRequest request
    ) {
        ApplicationMessage.SenderRole senderRole = applicationAccessService.requireChatWriteAccess(applicationId, currentUserId);
        String body = cleanBody(request.body());
        List<UUID> attachmentIds = request.attachmentIds() == null ? List.of() : request.attachmentIds();

        if (body == null && attachmentIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Message body or attachments are required");
        }
        if (body != null && body.length() > 4000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "body max length is 4000");
        }

        List<StoredFile> attachments = validateAttachments(applicationId, currentUserId, attachmentIds);

        ApplicationMessage message = new ApplicationMessage();
        message.setApplicationId(applicationId);
        message.setSenderId(currentUserId);
        message.setSenderRole(senderRole);
        message.setBody(body == null ? "" : body);

        ApplicationMessage saved = applicationMessageRepository.save(message);
        for (StoredFile attachment : attachments) {
            MessageAttachment messageAttachment = new MessageAttachment();
            messageAttachment.setMessageId(saved.getId());
            messageAttachment.setFileId(attachment.getId());
            messageAttachmentRepository.save(messageAttachment);
        }

        return buildChatItems(List.of(saved)).getFirst();
    }

    private List<ApplicationMessageDtos.ChatMessageItem> buildChatItems(List<ApplicationMessage> messages) {
        if (messages.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> senderNamesById = loadSenderNames(messages);
        Map<UUID, List<FileDtos.AttachmentItem>> attachmentsByMessageId = loadAttachments(messages);

        return messages.stream()
                .map(message -> new ApplicationMessageDtos.ChatMessageItem(
                        message.getId(),
                        message.getApplicationId(),
                        message.getSenderId(),
                        message.getSenderRole().name(),
                        senderNamesById.getOrDefault(parseUuidOrNull(message.getSenderId()), "Unknown"),
                        message.getBody(),
                        attachmentsByMessageId.getOrDefault(message.getId(), List.of()),
                        message.getCreatedAt()
                ))
                .toList();
    }

    private Map<UUID, String> loadSenderNames(List<ApplicationMessage> messages) {
        Set<UUID> senderIds = messages.stream()
                .map(ApplicationMessage::getSenderId)
                .map(this::parseUuidOrNull)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        return profileRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(
                        UserProfile::getId,
                        profile -> profile.getFullName() != null && !profile.getFullName().isBlank() ? profile.getFullName() : "Unknown"
                ));
    }

    private Map<UUID, List<FileDtos.AttachmentItem>> loadAttachments(List<ApplicationMessage> messages) {
        List<MessageAttachment> messageAttachments = messageAttachmentRepository.findByMessageIdIn(
                messages.stream().map(ApplicationMessage::getId).toList()
        );
        if (messageAttachments.isEmpty()) {
            return Map.of();
        }

        Map<UUID, StoredFile> filesById = storedFileService.loadFilesByIds(
                messageAttachments.stream().map(MessageAttachment::getFileId).distinct().toList()
        );

        Map<UUID, List<FileDtos.AttachmentItem>> attachmentsByMessageId = new LinkedHashMap<>();
        for (MessageAttachment messageAttachment : messageAttachments.stream()
                .sorted(Comparator.comparing(MessageAttachment::getCreatedAt))
                .toList()) {
            StoredFile file = filesById.get(messageAttachment.getFileId());
            if (file == null || file.getStatus() != StoredFileStatus.READY) {
                continue;
            }
            attachmentsByMessageId
                    .computeIfAbsent(messageAttachment.getMessageId(), ignored -> new ArrayList<>())
                    .add(storedFileService.toAttachmentItem(file));
        }
        return attachmentsByMessageId;
    }

    private List<StoredFile> validateAttachments(UUID applicationId, String currentUserId, Collection<UUID> attachmentIds) {
        if (attachmentIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, StoredFile> filesById = storedFileService.loadFilesByIds(List.copyOf(attachmentIds));
        List<StoredFile> attachments = new ArrayList<>();
        for (UUID attachmentId : attachmentIds) {
            StoredFile file = filesById.get(attachmentId);
            if (file == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Attachment not found");
            }
            if (file.getPurpose() != StoredFilePurpose.CHAT_ATTACHMENT ||
                    file.getStatus() != StoredFileStatus.READY ||
                    !currentUserId.equals(file.getOwnerUserId()) ||
                    !applicationId.equals(file.getApplicationId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Attachment cannot be used in this chat");
            }
            attachments.add(file);
        }
        return attachments;
    }

    private String cleanBody(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
