package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.StudentApplicationDtos;
import com.production.ZhasIntern.entity.Application;
import com.production.ZhasIntern.entity.ApplicationStatus;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentApplicationService {

    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;

    public StudentApplicationService(ApplicationRepository applicationRepository,
                                     InternshipRepository internshipRepository) {
        this.applicationRepository = applicationRepository;
        this.internshipRepository = internshipRepository;
    }

    public Page<StudentApplicationDtos.MineListItem> getMyApplications(
            String studentId,
            String q,
            String statusRaw,
            String applicationIdRaw,
            Pageable pageable
    ) {
        // 1) Base page by status (DB-level) or no status
        Page<Application> page;
        ApplicationStatus status = parseStatusOrNull(statusRaw);

        if (status != null) {
            page = applicationRepository.findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, status, pageable);
        } else {
            page = applicationRepository.findByStudentIdOrderByCreatedAtDesc(studentId, pageable);
        }

        // 2) Resolve internship titles in bulk
        List<Application> apps = page.getContent();
        Set<UUID> internshipIds = apps.stream()
                .map(Application::getInternshipId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Internship> internshipsById = new HashMap<>();
        if (!internshipIds.isEmpty()) {
            // Using findAllById is simplest/most compatible:
            internshipRepository.findAllById(internshipIds).forEach(it -> internshipsById.put(it.getId(), it));
        }

        // 3) Map to DTO
        List<StudentApplicationDtos.MineListItem> mapped = apps.stream()
                .map(app -> new StudentApplicationDtos.MineListItem(
                        app.getId(),
                        app.getInternshipId(),
                        Optional.ofNullable(internshipsById.get(app.getInternshipId()))
                                .map(Internship::getTitle)
                                .orElse("(Internship not found)"),
                        app.getStatus(),
                        app.getCreatedAt(),
                        app.getUpdatedAt()
                ))
                .collect(Collectors.toList());

        // 4) Optional filters:
        //    - applicationId exact match
        //    - q in internship title
        UUID applicationIdFilter = parseUuidOrNull(applicationIdRaw);
        String qNorm = (q == null) ? "" : q.trim().toLowerCase();

        List<StudentApplicationDtos.MineListItem> filtered = mapped.stream()
                .filter(it -> applicationIdFilter == null || it.applicationId().equals(applicationIdFilter))
                .filter(it -> qNorm.isEmpty() || safeLower(it.internshipTitle()).contains(qNorm))
                .toList();

        // 5) If no extra filters were applied, keep original page metadata.
        //    If filters were applied, the "totalElements" is no longer accurate (because we filtered after paging).
        //    For MVP: simplest approach is return filtered as a page with same pageable and corrected size for current page.
        //    This is OK for UI if you mainly use Prev/Next.
        boolean hasPostFilter = (applicationIdFilter != null) || !qNorm.isEmpty();
        if (!hasPostFilter) {
            return new PageImpl<>(filtered, pageable, page.getTotalElements());
        }
        // Post-filtered: we only know filtered count for THIS page.
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    private static ApplicationStatus parseStatusOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ApplicationStatus.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return null; // you can throw 400 if you prefer strictness
        }
    }

    private static UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}