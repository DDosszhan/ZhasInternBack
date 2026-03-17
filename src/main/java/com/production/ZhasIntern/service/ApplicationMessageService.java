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

    public List<ApplicationMessageDtos.ChatMessageItem> listMessages(UUID applicationId, String currentUserId) {
        AccessContext access = resolveAccess(applicationId, currentUserId);

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
        AccessContext access = resolveAccess(applicationId, currentUserId);

        ApplicationMessage message = new ApplicationMessage();
        message.setApplicationId(applicationId);
        message.setSenderId(currentUserId);
        message.setSenderRole(access.senderRole());
        message.setBody(request.body().trim());

        ApplicationMessage saved = applicationMessageRepository.save(message);
        return toItem(saved);
    }

    private AccessContext resolveAccess(UUID applicationId, String currentUserId) {
        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Application not found"));

        Internship internship = internshipRepository.findById(application.getInternshipId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));

        if (currentUserId.equals(application.getStudentId())) {
            return new AccessContext(ApplicationMessage.SenderRole.STUDENT);
        }

        if (currentUserId.equals(internship.getEmployerId())) {
            return new AccessContext(ApplicationMessage.SenderRole.EMPLOYER);
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this application chat");
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