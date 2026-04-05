package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.ApplicationMessageDtos;
import com.production.ZhasIntern.entity.ApplicationMessage;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ApplicationMessageRepository;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationMessageService {

    private final ApplicationMessageRepository applicationMessageRepository;
    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;
    private final ProfileRepository profileRepository;
    private final SchoolCounselorService schoolCounselorService;

    public List<ApplicationMessageDtos.ChatMessageItem> listMessages(UUID applicationId, String currentUserId) {
        ensureReadAccess(applicationId, currentUserId);

        return applicationMessageRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream()
                .map(this::toItem)
                .toList();
    }

    public ApplicationMessageDtos.ChatMessageItem createMessage(
            UUID applicationId,
            String currentUserId,
            ApplicationMessageDtos.CreateChatMessageRequest request
    ) {
        AccessContext access = resolveWriteAccess(applicationId, currentUserId);

        ApplicationMessage message = new ApplicationMessage();
        message.setApplicationId(applicationId);
        message.setSenderId(currentUserId);
        message.setSenderRole(access.senderRole());
        message.setBody(request.body().trim());

        ApplicationMessage saved = applicationMessageRepository.save(message);
        return toItem(saved);
    }

    private void ensureReadAccess(UUID applicationId, String currentUserId) {
        var application = loadApplication(applicationId);
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

    private AccessContext resolveWriteAccess(UUID applicationId, String currentUserId) {
        var application = loadApplication(applicationId);
        Internship internship = loadInternship(application.getInternshipId());

        if (currentUserId.equals(application.getStudentId())) {
            return new AccessContext(ApplicationMessage.SenderRole.STUDENT);
        }

        if (currentUserId.equals(internship.getEmployerId())) {
            return new AccessContext(ApplicationMessage.SenderRole.EMPLOYER);
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to write in this application chat");
    }

    private com.production.ZhasIntern.entity.Application loadApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Application not found"));
    }

    private Internship loadInternship(UUID internshipId) {
        return internshipRepository.findById(internshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));
    }

    private UserProfile loadProfile(String currentUserId) {
        return profileRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));
    }

    private ApplicationMessageDtos.ChatMessageItem toItem(ApplicationMessage message) {
        String senderName = profileRepository.findById(UUID.fromString(message.getSenderId()))
                .map(UserProfile::getFullName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Unknown");

        return new ApplicationMessageDtos.ChatMessageItem(
                message.getId(),
                message.getApplicationId(),
                message.getSenderId(),
                message.getSenderRole().name(),
                senderName,
                message.getBody(),
                message.getCreatedAt()
        );
    }

    private record AccessContext(ApplicationMessage.SenderRole senderRole) {}
}
