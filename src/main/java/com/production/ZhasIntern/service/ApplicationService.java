package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.ApplicationDtos;
import com.production.ZhasIntern.entity.Application;
import com.production.ZhasIntern.entity.ApplicationStatus;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository appRepo;
    private final InternshipRepository internshipRepo;
    private final ProfileRepository profileRepository;

    public ApplicationService(
            ApplicationRepository appRepo,
            InternshipRepository internshipRepo,
            ProfileRepository profileRepository
    ) {
        this.appRepo = appRepo;
        this.internshipRepo = internshipRepo;
        this.profileRepository = profileRepository;
    }

    // =========================
    // Student apply
    // =========================

    public ApplicationDtos.CreateResponse apply(String studentId, UUID internshipId, ApplicationDtos.CreateRequest req) {
        Internship it = internshipRepo.findByIdAndStatus(internshipId, Internship.Status.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));

        if (appRepo.existsByInternshipIdAndStudentId(internshipId, studentId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_APPLIED", "You already applied to this internship");
        }

        Map<String, Object> answers = (req != null && req.answers() != null) ? req.answers() : Map.of();

        Application app = new Application();
        app.setInternshipId(it.getId());
        app.setStudentId(studentId);
        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setAnswers(new HashMap<>(answers));

        try {
            Application saved = appRepo.save(app);
            return new ApplicationDtos.CreateResponse(saved.getId());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_APPLIED", "You already applied to this internship");
        }
    }

    // =========================
    // Employer view applicants (with optional status filter)
    // =========================

    public Page<ApplicationDtos.EmployerListItem> listApplicantsForInternship(
            String employerId,
            UUID internshipId,
            String status, // keep string input from query param
            Pageable pageable
    ) {
        Internship it = internshipRepo.findByIdAndEmployerId(internshipId, employerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));

        Page<Application> page;
        if (status != null && !status.isBlank()) {
            ApplicationStatus st = parseStatus(status);
            page = appRepo.findByInternshipIdAndStatusOrderByCreatedAtDesc(internshipId, st, pageable);
        } else {
            page = appRepo.findByInternshipIdOrderByCreatedAtDesc(internshipId, pageable);
        }

        Set<UUID> studentIds = page.getContent().stream()
                .map(Application::getStudentId)
                .map(this::parseUuidOrNull)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> studentNamesById = profileRepository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(
                        UserProfile::getId,
                        p -> p.getFullName() != null ? p.getFullName() : "",
                        (left, right) -> left
                ));

        return page.map(app -> {
            String studentId = app.getStudentId();
            UUID studentUuid = parseUuidOrNull(studentId);
            String studentFullName = studentUuid == null ? null : studentNamesById.get(studentUuid);
            if (studentFullName != null && studentFullName.isBlank()) {
                studentFullName = null;
            }

            String statusName = app.getStatus() != null ? app.getStatus().name() : "SUBMITTED";

            String studentProfilePath = (studentId != null && !studentId.isBlank())
                    ? "/students/" + studentId
                    : null;

            return new ApplicationDtos.EmployerListItem(
                    app.getId(),
                    app.getInternshipId(),
                    it.getTitle(),
                    studentId,
                    studentFullName,
                    studentProfilePath,
                    statusName,
                    app.getCreatedAt(),
                    app.getAnswers() != null ? app.getAnswers() : Map.of()
            );
        });
    }

    // =========================
    // Employer update status
    // =========================

    public ApplicationDtos.UpdateStatusResponse updateStatus(
            String employerId,
            UUID internshipId,
            UUID applicationId,
            String newStatusRaw
    ) {
        internshipRepo.findByIdAndEmployerId(internshipId, employerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));

        Application app = appRepo.findByIdAndInternshipId(applicationId, internshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Application not found"));

        ApplicationStatus newStatus = parseStatus(newStatusRaw);

        if (newStatus == app.getStatus()) {
            return new ApplicationDtos.UpdateStatusResponse(app.getId(), app.getStatus().name(), app.getUpdatedAt());
        }

        app.setStatus(newStatus);
        Application saved = appRepo.save(app); // updatedAt will be set by @PreUpdate

        return new ApplicationDtos.UpdateStatusResponse(saved.getId(), saved.getStatus().name(), saved.getUpdatedAt());
    }

    private UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private ApplicationStatus parseStatus(String raw) {
        if (raw == null) throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Status is required");
        try {
            return ApplicationStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid status. Allowed: SUBMITTED, ACCEPTED, REJECTED");
        }
    }
}
