package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.ProfileDtos;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ProfileRepository;
import com.production.ZhasIntern.security.AccessPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileRepository profileRepository;
    private final AccessPolicyService accessPolicyService;

    @PatchMapping("/role")
    public ProfileDtos.ProfileRoleResponse updateRole(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ProfileDtos.UpdateRoleRequest request
    ) {
        String userId = jwt.getSubject();
        UserRole role = accessPolicyService.parseAllowedSelfSwitchRole(request.role());

        UserProfile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));

        profile.setRole(role);
        profileRepository.save(profile);

        return new ProfileDtos.ProfileRoleResponse(userId, role.name());
    }
}