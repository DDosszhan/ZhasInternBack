package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.ApplicationMessageDtos;
import com.production.ZhasIntern.service.ApplicationMessageService;
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
}