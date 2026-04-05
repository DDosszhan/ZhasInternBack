package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.StudentApplicationDtos;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.StudentApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class StudentApplicationController {

    private final StudentApplicationService studentApplicationService;
    private final AccessPolicyService accessPolicyService;

    // ✅ aliases to match frontend fallback order
    @GetMapping({"/applications/mine", "/student/applications/mine", "/students/applications/mine"})
    public Page<StudentApplicationDtos.MineListItem> mine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "applicationId", required = false) String applicationId,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable
    ) {
        accessPolicyService.requireStudent(jwt);
        String studentId = jwt.getSubject();
        return studentApplicationService.getMyApplications(studentId, q, status, applicationId, pageable);
    }
}
