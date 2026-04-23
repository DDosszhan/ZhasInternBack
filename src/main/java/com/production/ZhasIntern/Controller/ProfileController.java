package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.ProfileDtos;
import com.production.ZhasIntern.dto.FileDtos;
import com.production.ZhasIntern.entity.ManualSchoolRequest;
import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ManualSchoolRequestRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import com.production.ZhasIntern.repository.SchoolRepository;
import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.StoredFileService;
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
    private final SchoolRepository schoolRepository;
    private final ManualSchoolRequestRepository manualSchoolRequestRepository;
    private final StoredFileService storedFileService;

    @GetMapping("/me")
    public ProfileDtos.MeResponse getCurrentProfile(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        UserProfile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));

        return new ProfileDtos.MeResponse(
                profile.getId().toString(),
                profile.getEmail(),
                profile.getFullName(),
                profile.getRole() != null ? profile.getRole().name() : null,
                profile.getBio(),
                profile.getPhone(),
                profile.getSchool(),
                profile.getGrade(),
                profile.getCity(),
                profile.getPortfolio(),
                profile.getSchoolEntity() != null ? profile.getSchoolEntity().getId().toString() : null,
                profile.getManualSchoolName(),
                profile.isSchoolCounselorVerified(),
                storedFileService.resolveCurrentUserProfilePhotoUrl(profile.getProfilePhotoFileId(), userId)
        );
    }

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
        profile.setSchoolCounselorVerified(false);
        profile.setSchoolCounselorVerifiedAt(null);
        profileRepository.save(profile);

        return new ProfileDtos.ProfileRoleResponse(userId, role.name());
    }

    @PatchMapping("/student-details")
    public ProfileDtos.StudentDetailsResponse updateStudentDetails(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ProfileDtos.UpdateStudentDetailsRequest request
    ) {
        String userId = jwt.getSubject();
        UserProfile profile = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));

        if (profile.getRole() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only student profile can be updated here");
        }

        if (request.fullName() != null) profile.setFullName(clean(request.fullName()));
        if (request.bio() != null) profile.setBio(clean(request.bio()));
        if (request.phone() != null) profile.setPhone(clean(request.phone()));
        if (request.grade() != null) profile.setGrade(clean(request.grade()));
        if (request.city() != null) profile.setCity(clean(request.city()));
        if (request.portfolio() != null) profile.setPortfolio(clean(request.portfolio()));

        if (request.school() != null) {
            profile.setSchool(clean(request.school()));
        }

        applySchoolChoice(profile, request);
        validateStudentSchoolSelection(profile);

        UserProfile saved = profileRepository.save(profile);

        return new ProfileDtos.StudentDetailsResponse(
                saved.getId().toString(),
                saved.getFullName(),
                saved.getBio(),
                saved.getPhone(),
                saved.getSchool(),
                saved.getGrade(),
                saved.getCity(),
                saved.getPortfolio(),
                saved.getSchoolEntity() != null ? saved.getSchoolEntity().getId().toString() : null,
                saved.getManualSchoolName(),
                saved.getSchoolEntity() != null ? clean(saved.getSchoolEntity().getRegionRu()) : null,
                saved.getSchoolEntity() != null ? clean(saved.getSchoolEntity().getDistrictRu()) : null,
                saved.getSchoolEntity() != null ? clean(saved.getSchoolEntity().getLocalityRu()) : null,
                storedFileService.resolveProfilePhotoUrl(saved.getProfilePhotoFileId())
        );
    }

    @PostMapping("/photo/upload-url")
    @ResponseStatus(HttpStatus.CREATED)
    public FileDtos.UploadTargetResponse createProfilePhotoUploadUrl(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @jakarta.validation.Valid FileDtos.InitiateUploadRequest request
    ) {
        return storedFileService.createProfilePhotoUploadTarget(jwt.getSubject(), request);
    }

    @PostMapping("/photo/{fileId}/complete")
    public FileDtos.CompleteUploadResponse completeProfilePhotoUpload(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return storedFileService.completeProfilePhotoUpload(fileId, jwt.getSubject());
    }

    private void applySchoolChoice(UserProfile profile, ProfileDtos.UpdateStudentDetailsRequest request) {
        String schoolIdRaw = clean(request.schoolId());
        String manualSchoolName = clean(request.manualSchoolName());

        if (schoolIdRaw == null && manualSchoolName == null) {
            return;
        }

        if (schoolIdRaw != null && manualSchoolName != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Provide either schoolId or manualSchoolName, not both");
        }

        if (schoolIdRaw != null) {
            UUID schoolId;
            try {
                schoolId = UUID.fromString(schoolIdRaw);
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "schoolId must be a valid UUID");
            }

            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "School not found"));

            profile.setSchoolEntity(school);
            profile.setManualSchoolName(null);
            profile.setSchool(school.getSchoolNameRu() != null ? school.getSchoolNameRu() : school.getSchoolNameKz());
            return;
        }

        profile.setSchoolEntity(null);
        profile.setManualSchoolName(manualSchoolName);
        profile.setSchool(manualSchoolName);

        ManualSchoolRequest manualRequest = new ManualSchoolRequest();
        manualRequest.setUserId(profile.getId());
        manualRequest.setRegion(clean(request.schoolRegion()));
        manualRequest.setDistrict(clean(request.schoolDistrict()));
        manualRequest.setLocality(clean(request.schoolLocality()));
        manualRequest.setSchoolName(manualSchoolName);

        manualSchoolRequestRepository.save(manualRequest);
    }

    private void validateStudentSchoolSelection(UserProfile profile) {
        boolean hasSelectedSchool = profile.getSchoolEntity() != null;
        boolean hasManualSchool = profile.getManualSchoolName() != null && !profile.getManualSchoolName().isBlank();

        if (!hasSelectedSchool && !hasManualSchool) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Student must select a school or provide a manual school name");
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
