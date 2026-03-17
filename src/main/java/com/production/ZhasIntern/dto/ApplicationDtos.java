package com.production.ZhasIntern.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ApplicationDtos {

    // Student submits optional answers
    public record CreateRequest(
            Map<String, Object> answers
    ) {}

    public record CreateResponse(
            UUID id
    ) {}

    // Employer views applicants for one internship they own
    public record EmployerListItem(
            UUID id,
            UUID internshipId,
            String internshipTitle,
            String studentId,
            String studentFullName,
            String studentProfilePath,
            String status,
            Instant createdAt,
            Map<String, Object> answers
    ) {}

    // Employer updates applicant status
    public record UpdateStatusRequest(
            String status
    ) {}

    public record UpdateStatusResponse(
            UUID id,
            String status,
            Instant updatedAt
    ) {}
}
