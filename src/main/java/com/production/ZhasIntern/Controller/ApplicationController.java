package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.ApplicationDtos;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AccessPolicyService accessPolicyService;

    @PostMapping("/internships/{id}/apply")
    public ApplicationDtos.CreateResponse apply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID internshipId,
            @RequestBody(required = false) ApplicationDtos.CreateRequest req
    ) {
        accessPolicyService.requireStudent(jwt);
        String studentId = jwt.getSubject();
        return applicationService.apply(studentId, internshipId, req);
    }

    @GetMapping("/employer/internships/{id}/applications")
    public Page<ApplicationDtos.EmployerListItem> listApplicants(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID internshipId,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        accessPolicyService.requireEmployer(jwt);
        String employerId = jwt.getSubject();
        return applicationService.listApplicantsForInternship(employerId, internshipId, status, pageable);
    }
}
