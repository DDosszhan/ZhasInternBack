package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.StudentProfileDtos;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final ProfileRepository profileRepository;
    private final ApplicationRepository applicationRepository;

    public StudentProfileDtos.StudentProfileResponse getStudentProfile(UUID studentId, String currentUserId) {
        UserProfile currentUserProfile = profileRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));

        boolean isSelf = currentUserId.equals(studentId.toString());
        boolean canAccess = false;

        if (currentUserProfile.getRole() == UserRole.EMPLOYER) {
            canAccess = applicationRepository.existsEmployerAccessToStudent(currentUserId, studentId.toString());
        } else if (currentUserProfile.getRole() == UserRole.STUDENT && isSelf) {
            canAccess = true;
        }

        if (!canAccess) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this student profile");
        }

        UserProfile studentProfile = profileRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Student profile not found"));

        return new StudentProfileDtos.StudentProfileResponse(
                studentProfile.getId(),
                studentProfile.getFullName(),
                studentProfile.getBio(),
                null,
                null,
                null,
                null,
                studentProfile.getEmail(),
                List.of(),
                null
        );
    }
}
