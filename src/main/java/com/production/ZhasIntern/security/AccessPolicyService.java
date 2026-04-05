package com.production.ZhasIntern.security;

import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessPolicyService {

    private final ProfileRepository profileRepository;

    public UserProfile requireCurrentProfile(Jwt jwt) {
        String userId = jwt.getSubject();
        return profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        "Profile not found"
                ));
    }

    public void requireEmployer(Jwt jwt) {
        UserProfile profile = requireCurrentProfile(jwt);

        if (profile.getRole() != UserRole.EMPLOYER) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Employer role is required"
            );
        }
    }


    public void requireAdmin(Jwt jwt) {
        UserProfile profile = requireCurrentProfile(jwt);

        if (profile.getRole() != UserRole.ADMIN) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Admin role is required"
            );
        }
    }

    public void requireStudent(Jwt jwt) {
        UserProfile profile = requireCurrentProfile(jwt);

        if (profile.getRole() != UserRole.STUDENT) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Student role is required"
            );
        }
    }

    public void requireCounselorApprover(Jwt jwt) {
        UserProfile profile = requireCurrentProfile(jwt);

        if (profile.getRole() != UserRole.COUNSELOR_APPROVER && profile.getRole() != UserRole.ADMIN) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "Counselor approver role is required"
            );
        }
    }

    public UserProfile requireApprovedSchoolCounselor(Jwt jwt) {
        UserProfile profile = requireCurrentProfile(jwt);

        if (profile.getRole() != UserRole.SCHOOL_COUNSELOR) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "School counselor role is required"
            );
        }

        if (!profile.isSchoolCounselorVerified()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "COUNSELOR_NOT_VERIFIED",
                    "School counselor account is not verified yet"
            );
        }

        return profile;
    }

    public UserRole parseAllowedSelfSwitchRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Role is required");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Invalid role",
                    Map.of("allowedRoles", new String[]{"STUDENT", "EMPLOYER", "SCHOOL_COUNSELOR"})
            );
        }

        if (role == UserRole.ADMIN || role == UserRole.COUNSELOR_APPROVER) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "You cannot assign this role to yourself"
            );
        }

        return role;
    }
}
