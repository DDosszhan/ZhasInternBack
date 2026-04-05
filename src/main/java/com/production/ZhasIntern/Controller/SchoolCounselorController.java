package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.SchoolCounselorDtos;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.SchoolCounselorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SchoolCounselorController {

    private final SchoolCounselorService schoolCounselorService;
    private final AccessPolicyService accessPolicyService;

    @PostMapping("/school-counselor/verification-request")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolCounselorDtos.VerificationRequestResponse submitVerificationRequest(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody SchoolCounselorDtos.SubmitVerificationRequest request
    ) {
        return schoolCounselorService.submitVerificationRequest(jwt.getSubject(), request);
    }

    @GetMapping("/school-counselor/verification-request")
    public SchoolCounselorDtos.VerificationRequestListItem getOwnLatestVerificationRequest(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return schoolCounselorService.getLatestOwnRequest(jwt.getSubject());
    }

    @GetMapping("/school-counselor/students")
    public Page<SchoolCounselorDtos.SchoolStudentListItem> listStudents(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UserProfile counselorProfile = accessPolicyService.requireApprovedSchoolCounselor(jwt);
        return schoolCounselorService.listStudentsForCounselor(counselorProfile, pageable);
    }

    @GetMapping("/school-counselor/students/{studentId}/applications")
    public Page<SchoolCounselorDtos.SchoolStudentApplicationItem> listStudentApplications(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID studentId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UserProfile counselorProfile = accessPolicyService.requireApprovedSchoolCounselor(jwt);
        return schoolCounselorService.listStudentApplicationsForCounselor(counselorProfile, studentId, pageable);
    }

    @GetMapping("/counselor-approvals/requests")
    public Page<SchoolCounselorDtos.VerificationRequestListItem> listVerificationRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        accessPolicyService.requireCounselorApprover(jwt);
        return schoolCounselorService.listVerificationRequests(status, pageable);
    }

    @PatchMapping("/counselor-approvals/requests/{requestId}")
    public SchoolCounselorDtos.ReviewVerificationResponse reviewVerificationRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId,
            @RequestBody SchoolCounselorDtos.ReviewVerificationRequest request
    ) {
        accessPolicyService.requireCounselorApprover(jwt);
        return schoolCounselorService.reviewRequest(requestId, jwt.getSubject(), request);
    }
}
