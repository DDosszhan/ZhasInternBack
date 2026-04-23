package com.production.ZhasIntern.service;

import com.production.ZhasIntern.entity.Application;
import com.production.ZhasIntern.entity.ApplicationMessage;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationAccessService {

    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;
    private final ProfileRepository profileRepository;
    private final SchoolCounselorService schoolCounselorService;

    public void ensureCanReadChat(UUID applicationId, String currentUserId) {
        Application application = loadApplication(applicationId);
        Internship internship = loadInternship(application.getInternshipId());

        if (currentUserId.equals(application.getStudentId()) || currentUserId.equals(internship.getEmployerId())) {
            return;
        }

        UserProfile currentUserProfile = loadProfile(currentUserId);
        if (currentUserProfile.getRole() == UserRole.SCHOOL_COUNSELOR && currentUserProfile.isSchoolCounselorVerified()) {
            schoolCounselorService.ensureCounselorCanAccessStudent(currentUserProfile, UUID.fromString(application.getStudentId()));
            return;
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this application chat");
    }

    public ApplicationMessage.SenderRole requireChatWriteAccess(UUID applicationId, String currentUserId) {
        Application application = loadApplication(applicationId);
        Internship internship = loadInternship(application.getInternshipId());

        if (currentUserId.equals(application.getStudentId())) {
            return ApplicationMessage.SenderRole.STUDENT;
        }

        if (currentUserId.equals(internship.getEmployerId())) {
            return ApplicationMessage.SenderRole.EMPLOYER;
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to write in this application chat");
    }

    public Application loadApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Application not found"));
    }

    public Internship loadInternship(UUID internshipId) {
        return internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));
    }

    public UserProfile loadProfile(String currentUserId) {
        return profileRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));
    }
}
