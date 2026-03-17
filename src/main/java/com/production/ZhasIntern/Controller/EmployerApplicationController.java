package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.EmployerApplicationDtos;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.EmployerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employer/internships/{internshipId}/applications")
public class EmployerApplicationController {

    private final EmployerApplicationService service;
    private final AccessPolicyService accessPolicyService;

    @PatchMapping("/{applicationId}/status")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void updateStatus(
            @PathVariable UUID internshipId,
            @PathVariable UUID applicationId,
            @RequestBody @Valid EmployerApplicationDtos.UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        accessPolicyService.requireEmployer(jwt);
        String employerUserId = jwt.getSubject();
        service.updateStatus(internshipId, applicationId, request.status(), employerUserId);
    }
}