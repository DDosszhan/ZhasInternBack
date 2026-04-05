package com.production.ZhasIntern.dto;

public class ProfileDtos {

    public record UpdateRoleRequest(String role) {}

    public record UpdateStudentDetailsRequest(
            String fullName,
            String bio,
            String phone,
            String school,
            String grade,
            String city,
            String portfolio,
            String schoolId,
            String manualSchoolName,
            String schoolRegion,
            String schoolDistrict,
            String schoolLocality
    ) {}

    public record ProfileRoleResponse(
            String userId,
            String role
    ) {}

    public record StudentDetailsResponse(
            String userId,
            String fullName,
            String bio,
            String phone,
            String school,
            String grade,
            String city,
            String portfolio,
            String schoolId,
            String manualSchoolName,
            String schoolRegion,
            String schoolDistrict,
            String schoolLocality
    ) {}
}
