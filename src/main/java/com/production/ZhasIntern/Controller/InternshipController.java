package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.InternshipDtos;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InternshipController {

    private final InternshipService internshipService;
    private final AccessPolicyService accessPolicyService;

    @GetMapping("/api/internships/public")
    public Page<InternshipDtos.PublicItem> listPublic(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return internshipService.listPublic(pageable);
    }

    @GetMapping("/api/internships/public/{id}")
    public InternshipDtos.PublicDetails getPublic(@PathVariable java.util.UUID id) {
        return internshipService.getPublic(id);
    }

    @GetMapping("/api/employer/internships")
    public Page<InternshipDtos.MineItem> listEmployerInternships(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        accessPolicyService.requireEmployer(jwt);
        String employerId = jwt.getSubject();
        return internshipService.listMine(employerId, pageable);
    }

    @PostMapping("/api/employer/internships")
    @ResponseStatus(HttpStatus.CREATED)
    public InternshipDtos.CreateResponse createEmployerInternship(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid InternshipDtos.CreateRequest req
    ) {
        accessPolicyService.requireEmployer(jwt);
        String employerId = jwt.getSubject();
        return new InternshipDtos.CreateResponse(
                internshipService.createEmployerInternship(employerId, req)
        );
    }

    @GetMapping("/api/internships/mine")
    public Page<InternshipDtos.MineItem> listMine(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        accessPolicyService.requireEmployer(jwt);
        String employerId = jwt.getSubject();
        return internshipService.listMine(employerId, pageable);
    }

    @PostMapping("/api/internships/{id}/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable java.util.UUID id
    ) {
        accessPolicyService.requireEmployer(jwt);
        String employerId = jwt.getSubject();
        internshipService.publish(employerId, id);
    }

    @GetMapping("/api/debug/auth")
    public Map<String, Object> debugAuth(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getClaims();
    }
}