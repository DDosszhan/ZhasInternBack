package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.ApplicationDtos;
import com.production.ZhasIntern.entity.Application;
import com.production.ZhasIntern.entity.ApplicationStatus;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository appRepo;
    private final InternshipRepository internshipRepo;

    public ApplicationService(ApplicationRepository appRepo, InternshipRepository internshipRepo) {
        this.appRepo = appRepo;
        this.internshipRepo = internshipRepo;
    }

    // =========================
    // Student apply
    // =========================

    public ApplicationDtos.CreateResponse apply(String studentId, UUID internshipId, ApplicationDtos.CreateRequest req) {
        Internship it = internshipRepo.findByIdAndStatus(internshipId, Internship.Status.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Internship not found"));

        if (appRepo.existsByInternshipIdAndStudentId(internshipId, studentId)) {
            throw new AlreadyAppliedException("You already applied to this internship");
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
            throw new AlreadyAppliedException("You already applied to this internship");
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
                .orElseThrow(() -> new NotFoundException("Internship not found"));

        Page<Application> page;
        if (status != null && !status.isBlank()) {
            ApplicationStatus st = parseStatus(status);
            page = appRepo.findByInternshipIdAndStatusOrderByCreatedAtDesc(internshipId, st, pageable);
        } else {
            page = appRepo.findByInternshipIdOrderByCreatedAtDesc(internshipId, pageable);
        }

        return page.map(app -> new ApplicationDtos.EmployerListItem(
                app.getId(),
                app.getInternshipId(),
                it.getTitle(),
                app.getStudentId(),
                app.getStatus().name(),          // or change DTO type to ApplicationStatus
                app.getCreatedAt(),
                app.getAnswers() != null ? app.getAnswers() : Map.of()
        ));
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
                .orElseThrow(() -> new NotFoundException("Internship not found"));

        Application app = appRepo.findByIdAndInternshipId(applicationId, internshipId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        ApplicationStatus newStatus = parseStatus(newStatusRaw);

        if (newStatus == app.getStatus()) {
            return new ApplicationDtos.UpdateStatusResponse(app.getId(), app.getStatus().name(), app.getUpdatedAt());
        }

        app.setStatus(newStatus);
        Application saved = appRepo.save(app); // updatedAt will be set by @PreUpdate

        return new ApplicationDtos.UpdateStatusResponse(saved.getId(), saved.getStatus().name(), saved.getUpdatedAt());
    }

    private ApplicationStatus parseStatus(String raw) {
        if (raw == null) throw new BadRequestException("Status is required");
        try {
            return ApplicationStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status. Allowed: SUBMITTED, ACCEPTED, REJECTED");
        }
    }

    // Exceptions
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String msg) { super(msg); }
    }

    public static class AlreadyAppliedException extends RuntimeException {
        public AlreadyAppliedException(String msg) { super(msg); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String msg) { super(msg); }
    }
}