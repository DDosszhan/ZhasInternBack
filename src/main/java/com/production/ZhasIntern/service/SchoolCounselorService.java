package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.SchoolCounselorDtos;
import com.production.ZhasIntern.entity.*;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import com.production.ZhasIntern.repository.SchoolCounselorVerificationRequestRepository;
import com.production.ZhasIntern.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolCounselorService {

    private final ProfileRepository profileRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolCounselorVerificationRequestRepository verificationRequestRepository;
    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;

    public SchoolCounselorDtos.VerificationRequestResponse submitVerificationRequest(
            String currentUserId,
            SchoolCounselorDtos.SubmitVerificationRequest request
    ) {
        UUID counselorId = parseUuid(currentUserId, "Invalid user id");
        UserProfile profile = profileRepository.findById(counselorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));

        if (verificationRequestRepository.existsByCounselorIdAndStatus(counselorId, SchoolCounselorRequestStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_PENDING", "A verification request is already pending");
        }

        AppliedSchool appliedSchool = resolveAppliedSchool(
                clean(request.schoolId()),
                clean(request.manualSchoolName()),
                clean(request.schoolRegion()),
                clean(request.schoolDistrict()),
                clean(request.schoolLocality())
        );

        String fullName = clean(request.fullName());
        String contactEmail = firstNonBlank(clean(request.contactEmail()), clean(profile.getEmail()));
        String contactPhone = firstNonBlank(clean(request.contactPhone()), clean(profile.getPhone()));

        if (fullName == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "fullName is required");
        }
        if (contactEmail == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "contactEmail is required");
        }
        if (contactPhone == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "contactPhone is required");
        }

        profile.setRole(UserRole.SCHOOL_COUNSELOR);
        profile.setFullName(fullName);
        profile.setEmail(contactEmail);
        profile.setPhone(contactPhone);
        profile.setSchoolEntity(appliedSchool.school());
        profile.setManualSchoolName(appliedSchool.school() == null ? appliedSchool.schoolName() : null);
        profile.setSchool(appliedSchool.schoolName());
        profile.setSchoolCounselorVerified(false);
        profile.setSchoolCounselorVerifiedAt(null);
        profileRepository.save(profile);

        SchoolCounselorVerificationRequest entity = new SchoolCounselorVerificationRequest();
        entity.setCounselorId(counselorId);
        entity.setSchoolEntity(appliedSchool.school());
        entity.setSchoolName(appliedSchool.schoolName());
        entity.setContactEmail(contactEmail);
        entity.setContactPhone(contactPhone);
        entity.setPositionTitle(clean(request.positionTitle()));
        entity.setContactsNote(buildContactsNote(clean(request.contactsNote()), appliedSchool));

        SchoolCounselorVerificationRequest saved = verificationRequestRepository.save(entity);
        return new SchoolCounselorDtos.VerificationRequestResponse(saved.getId(), saved.getStatus().name(), saved.getSubmittedAt());
    }

    public SchoolCounselorDtos.VerificationRequestListItem getLatestOwnRequest(String currentUserId) {
        UUID counselorId = parseUuid(currentUserId, "Invalid user id");
        SchoolCounselorVerificationRequest request = verificationRequestRepository.findTopByCounselorIdOrderBySubmittedAtDesc(counselorId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Verification request not found"));
        return toRequestListItem(request, loadProfilesById(Set.of(request.getCounselorId())));
    }

    public Page<SchoolCounselorDtos.VerificationRequestListItem> listVerificationRequests(String statusRaw, Pageable pageable) {
        Page<SchoolCounselorVerificationRequest> page;
        SchoolCounselorRequestStatus status = parseStatusOrNull(statusRaw);
        if (status == null) {
            page = verificationRequestRepository.findAllByOrderBySubmittedAtDesc(pageable);
        } else {
            page = verificationRequestRepository.findByStatusOrderBySubmittedAtDesc(status, pageable);
        }

        Map<UUID, UserProfile> profilesById = loadProfilesById(page.getContent().stream()
                .map(SchoolCounselorVerificationRequest::getCounselorId)
                .collect(Collectors.toSet()));

        return page.map(request -> toRequestListItem(request, profilesById));
    }

    public SchoolCounselorDtos.ReviewVerificationResponse reviewRequest(
            UUID requestId,
            String reviewerUserId,
            SchoolCounselorDtos.ReviewVerificationRequest request
    ) {
        SchoolCounselorVerificationRequest entity = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Verification request not found"));

        if (entity.getStatus() != SchoolCounselorRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_REVIEWED", "Verification request has already been reviewed");
        }

        UserProfile counselor = profileRepository.findById(entity.getCounselorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Counselor profile not found"));

        ReviewAction action = parseAction(request.action());
        Instant now = Instant.now();

        entity.setStatus(action == ReviewAction.APPROVE ? SchoolCounselorRequestStatus.APPROVED : SchoolCounselorRequestStatus.REJECTED);
        entity.setReviewComment(clean(request.reviewComment()));
        entity.setReviewedAt(now);
        entity.setReviewedBy(reviewerUserId);

        counselor.setRole(UserRole.SCHOOL_COUNSELOR);
        counselor.setSchoolEntity(entity.getSchoolEntity());
        counselor.setSchool(entity.getSchoolName());
        counselor.setSchoolCounselorVerified(action == ReviewAction.APPROVE);
        counselor.setSchoolCounselorVerifiedAt(action == ReviewAction.APPROVE ? now : null);

        verificationRequestRepository.save(entity);
        profileRepository.save(counselor);

        return new SchoolCounselorDtos.ReviewVerificationResponse(entity.getId(), entity.getStatus().name(), entity.getReviewedAt());
    }

    public Page<SchoolCounselorDtos.SchoolStudentListItem> listStudentsForCounselor(UserProfile counselorProfile, Pageable pageable) {
        Page<UserProfile> page = findStudentsByCounselorSchool(counselorProfile, pageable);
        return page.map(student -> new SchoolCounselorDtos.SchoolStudentListItem(
                student.getId(),
                student.getFullName(),
                student.getEmail(),
                student.getPhone(),
                student.getGrade(),
                student.getCity(),
                student.getSchool()
        ));
    }

    public Page<SchoolCounselorDtos.SchoolStudentApplicationItem> listStudentApplicationsForCounselor(
            UserProfile counselorProfile,
            UUID studentId,
            Pageable pageable
    ) {
        ensureCounselorCanAccessStudent(counselorProfile, studentId);

        Page<Application> page = applicationRepository.findByStudentIdOrderByCreatedAtDesc(studentId.toString(), pageable);
        Set<UUID> internshipIds = page.getContent().stream()
                .map(Application::getInternshipId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Internship> internshipsById = new HashMap<>();
        if (!internshipIds.isEmpty()) {
            internshipRepository.findAllById(internshipIds).forEach(it -> internshipsById.put(it.getId(), it));
        }

        return page.map(application -> {
            Internship internship = internshipsById.get(application.getInternshipId());
            return new SchoolCounselorDtos.SchoolStudentApplicationItem(
                    application.getId(),
                    application.getInternshipId(),
                    internship != null ? internship.getTitle() : "(Internship not found)",
                    internship != null ? internship.getEmployerId() : null,
                    application.getStatus() != null ? application.getStatus().name() : null,
                    application.getAnswers() != null ? application.getAnswers() : Map.of(),
                    application.getCreatedAt(),
                    application.getUpdatedAt()
            );
        });
    }

    public void ensureCounselorCanAccessStudent(UserProfile counselorProfile, UUID studentId) {
        UserProfile student = profileRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Student profile not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Target profile is not a student");
        }

        boolean sameSchool;
        if (counselorProfile.getSchoolEntity() != null) {
            sameSchool = student.getSchoolEntity() != null
                    && counselorProfile.getSchoolEntity().getId().equals(student.getSchoolEntity().getId());
        } else {
            String counselorSchool = normalizeSchoolName(counselorProfile.getSchool());
            String studentSchool = normalizeSchoolName(student.getSchool());
            sameSchool = counselorSchool != null && counselorSchool.equals(studentSchool);
        }

        if (!sameSchool) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this student");
        }
    }

    private Page<UserProfile> findStudentsByCounselorSchool(UserProfile counselorProfile, Pageable pageable) {
        if (counselorProfile.getSchoolEntity() != null) {
            return profileRepository.findByRoleAndSchoolEntity_IdOrderByFullNameAsc(
                    UserRole.STUDENT,
                    counselorProfile.getSchoolEntity().getId(),
                    pageable
            );
        }

        String schoolName = normalizeSchoolName(counselorProfile.getSchool());
        if (schoolName == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "COUNSELOR_SCHOOL_MISSING", "Verified counselor does not have a school assigned");
        }

        return profileRepository.findByRoleAndSchoolNameIgnoreCase(UserRole.STUDENT, schoolName, pageable);
    }

    private AppliedSchool resolveAppliedSchool(
            String schoolIdRaw,
            String manualSchoolName,
            String schoolRegion,
            String schoolDistrict,
            String schoolLocality
    ) {
        if (schoolIdRaw == null && manualSchoolName == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Provide schoolId or manualSchoolName");
        }
        if (schoolIdRaw != null && manualSchoolName != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Provide either schoolId or manualSchoolName, not both");
        }

        if (schoolIdRaw != null) {
            UUID schoolId = parseUuid(schoolIdRaw, "schoolId must be a valid UUID");
            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "School not found"));
            String schoolName = firstNonBlank(clean(school.getSchoolNameRu()), clean(school.getSchoolNameKz()));
            return new AppliedSchool(school, schoolName, null);
        }

        String normalizedManualName = clean(manualSchoolName);
        String composedNote = joinNonBlank(Arrays.asList(
                schoolRegion != null ? "Region: " + schoolRegion : null,
                schoolDistrict != null ? "District: " + schoolDistrict : null,
                schoolLocality != null ? "Locality: " + schoolLocality : null
        ));
        return new AppliedSchool(null, normalizedManualName, composedNote);
    }

    private SchoolCounselorDtos.VerificationRequestListItem toRequestListItem(
            SchoolCounselorVerificationRequest request,
            Map<UUID, UserProfile> profilesById
    ) {
        UserProfile profile = profilesById.get(request.getCounselorId());
        return new SchoolCounselorDtos.VerificationRequestListItem(
                request.getId(),
                request.getCounselorId().toString(),
                profile != null ? profile.getFullName() : null,
                profile != null ? profile.getEmail() : null,
                request.getContactEmail(),
                request.getContactPhone(),
                request.getPositionTitle(),
                request.getContactsNote(),
                request.getSchoolName(),
                request.getStatus().name(),
                request.getReviewComment(),
                request.getSubmittedAt(),
                request.getReviewedAt(),
                request.getReviewedBy()
        );
    }

    private Map<UUID, UserProfile> loadProfilesById(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(UserProfile::getId, profile -> profile));
    }

    private String buildContactsNote(String requestNote, AppliedSchool appliedSchool) {
        return joinNonBlank(Arrays.asList(requestNote, appliedSchool.schoolDetailsNote()));
    }

    private ReviewAction parseAction(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "action is required");
        }
        try {
            return ReviewAction.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid action. Allowed: APPROVE, REJECT");
        }
    }

    private SchoolCounselorRequestStatus parseStatusOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SchoolCounselorRequestStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid status. Allowed: PENDING, APPROVED, REJECTED");
        }
    }

    private UUID parseUuid(String raw, String message) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
        }
    }

    private String normalizeSchoolName(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private String joinNonBlank(List<String> parts) {
        return parts.stream()
                .filter(Objects::nonNull)
                .filter(part -> !part.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private enum ReviewAction {
        APPROVE,
        REJECT
    }

    private record AppliedSchool(School school, String schoolName, String schoolDetailsNote) {}
}
