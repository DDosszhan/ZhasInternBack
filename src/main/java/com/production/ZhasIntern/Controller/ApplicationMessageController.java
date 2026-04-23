package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.ApplicationMessageDtos;
import com.production.ZhasIntern.dto.FileDtos;
import com.production.ZhasIntern.service.ApplicationMessageService;
import com.production.ZhasIntern.service.StoredFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications/{applicationId}/messages")
public class ApplicationMessageController {

    private final ApplicationMessageService applicationMessageService;
    private final StoredFileService storedFileService;

    @GetMapping
    public List<ApplicationMessageDtos.ChatMessageItem> listMessages(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return applicationMessageService.listMessages(applicationId, jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationMessageDtos.ChatMessageItem createMessage(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ApplicationMessageDtos.CreateChatMessageRequest request
    ) {
        return applicationMessageService.createMessage(applicationId, jwt.getSubject(), request);
    }

    @PostMapping("/attachments/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public FileDtos.UploadTargetResponse createAttachmentUploadUrl(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid FileDtos.InitiateUploadRequest request
    ) {
        return storedFileService.createApplicationAttachmentUploadTarget(applicationId, jwt.getSubject(), request);
    }

    @PostMapping("/attachments/{fileId}/complete")
    public FileDtos.CompleteUploadResponse completeAttachmentUpload(
            @PathVariable UUID applicationId,
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return storedFileService.completeApplicationAttachmentUpload(applicationId, fileId, jwt.getSubject());
    }
}
