package com.production.ZhasIntern.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class SchoolCounselorDtos {

    public record SubmitVerificationRequest(
            String fullName,
            String contactEmail,
            String contactPhone,
            String positionTitle,
            String contactsNote,
            String schoolId,
            String manualSchoolName,
            String schoolRegion,
            String schoolDistrict,
            String schoolLocality
    ) {}

    public record VerificationRequestResponse(
            UUID id,
            String status,
            Instant submittedAt
    ) {}

    public record VerificationRequestListItem(
            UUID id,
            String counselorId,
            String counselorFullName,
            String counselorEmail,
            String contactEmail,
            String contactPhone,
            String positionTitle,
            String contactsNote,
            String schoolName,
            String status,
            String reviewComment,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewedBy
    ) {}

    public record ReviewVerificationRequest(
            String action,
            String reviewComment
    ) {}

    public record ReviewVerificationResponse(
            UUID id,
            String status,
            Instant reviewedAt
    ) {}

    public record SchoolStudentListItem(
            UUID id,
            String fullName,
            String email,
            String phone,
            String grade,
            String city,
            String school
    ) {}

    public record SchoolStudentApplicationItem(
            UUID applicationId,
            UUID internshipId,
            String internshipTitle,
            String employerId,
            String status,
            Map<String, Object> answers,
            Instant appliedAt,
            Instant updatedAt
    ) {}
}
