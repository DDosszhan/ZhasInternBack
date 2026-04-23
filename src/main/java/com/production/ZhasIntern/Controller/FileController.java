package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.FileDtos;
import com.production.ZhasIntern.service.StoredFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final StoredFileService storedFileService;

    @GetMapping("/{fileId}/download-url")
    public FileDtos.DownloadUrlResponse createDownloadUrl(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return storedFileService.createDownloadUrl(fileId, jwt.getSubject());
    }
}
