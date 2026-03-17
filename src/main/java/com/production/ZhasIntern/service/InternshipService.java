package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.InternshipDtos;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.InternshipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class InternshipService {

    private final InternshipRepository repo;

    public InternshipService(InternshipRepository repo) {
        this.repo = repo;
    }

    public Page<InternshipDtos.PublicItem> listPublic(Pageable pageable) {
        return repo.findByStatusOrderByPublishedAtDesc(Internship.Status.PUBLISHED, pageable)
                .map(this::toPublicItem);
    }

    public InternshipDtos.PublicDetails getPublic(UUID id) {
        Internship it = repo.findByIdAndStatus(id, Internship.Status.PUBLISHED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));
        return toPublicDetails(it);
    }

    public UUID createEmployerInternship(String employerId, InternshipDtos.CreateRequest req) {
        validateDeadline(req.deadline());

        Internship.WorkType type = parseType(req.type());

        Internship it = new Internship();
        it.setEmployerId(employerId);
        it.setTitle(req.title().trim());
        it.setCompanyName(req.companyName().trim());
        it.setLocation(req.location());
        it.setType(type);
        it.setIsRemote(req.isRemote());
        it.setShortDescription(req.shortDescription());
        it.setDescription(req.description());
        it.setDeadline(req.deadline());
        it.setStatus(Internship.Status.DRAFT);

        return repo.save(it).getId();
    }

    public Page<InternshipDtos.MineItem> listMine(String employerId, Pageable pageable) {
        return repo.findByEmployerIdOrderByCreatedAtDesc(employerId, pageable)
                .map(this::toMineItem);
    }

    public void publish(String employerId, UUID id) {
        Internship it = repo.findByIdAndEmployerId(id, employerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Internship not found"));

        if (it.getStatus() == Internship.Status.PUBLISHED) {
            return;
        }

        it.setStatus(Internship.Status.PUBLISHED);
        it.setPublishedAt(Instant.now());
        repo.save(it);
    }

    private void validateDeadline(Instant deadline) {
        if (deadline != null && deadline.isBefore(Instant.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "deadline cannot be in the past"
            );
        }
    }

    private Internship.WorkType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "type is required");
        }

        try {
            return Internship.WorkType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Invalid internship type"
            );
        }
    }

    private InternshipDtos.PublicItem toPublicItem(Internship it) {
        return new InternshipDtos.PublicItem(
                it.getId(),
                it.getTitle(),
                it.getStatus().name(),
                it.getCreatedAt(),
                it.getUpdatedAt(),
                it.getPublishedAt(),
                it.getDeadline(),
                it.getCompanyName(),
                it.getLocation(),
                it.getType() != null ? it.getType().name() : null,
                it.getIsRemote(),
                it.getShortDescription()
        );
    }

    private InternshipDtos.PublicDetails toPublicDetails(Internship it) {
        return new InternshipDtos.PublicDetails(
                it.getId(),
                it.getTitle(),
                it.getStatus().name(),
                it.getCreatedAt(),
                it.getUpdatedAt(),
                it.getPublishedAt(),
                it.getDeadline(),
                it.getCompanyName(),
                it.getLocation(),
                it.getType() != null ? it.getType().name() : null,
                it.getIsRemote(),
                it.getShortDescription(),
                it.getDescription()
        );
    }

    private InternshipDtos.MineItem toMineItem(Internship it) {
        return new InternshipDtos.MineItem(
                it.getId(),
                it.getTitle(),
                it.getStatus().name(),
                it.getCreatedAt(),
                it.getUpdatedAt(),
                it.getPublishedAt(),
                it.getDeadline(),
                it.getCompanyName(),
                it.getLocation(),
                it.getType() != null ? it.getType().name() : null,
                it.getIsRemote(),
                it.getShortDescription(),
                it.getDescription()
        );
    }
}