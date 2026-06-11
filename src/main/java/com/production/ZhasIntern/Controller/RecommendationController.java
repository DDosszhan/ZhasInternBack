package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.RecommendationDtos;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final AccessPolicyService accessPolicyService;
    private final RecommendationService recommendationService;

    @GetMapping("/api/recommendations/internships")
    public List<RecommendationDtos.RecommendedInternshipDto> recommendInternships(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserProfile student = accessPolicyService.requireCurrentProfile(jwt);
        accessPolicyService.requireStudent(jwt);
        return recommendationService.recommendInternships(student.getId());
    }
}
