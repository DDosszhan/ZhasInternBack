package com.production.ZhasIntern.dto;

import com.production.ZhasIntern.entity.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

public class StudentApplicationDtos {

    public record MineListItem(
            UUID applicationId,
            UUID internshipId,
            String internshipTitle,
            ApplicationStatus status,
            Instant appliedAt,
            Instant updatedAt
    ) {}
}