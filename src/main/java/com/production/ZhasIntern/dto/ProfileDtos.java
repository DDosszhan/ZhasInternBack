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

    public record MeResponse(
            String userId,
            String email,
            String fullName,
            String role,
            String bio,
            String phone,
            String school,
            String grade,
            String city,
            String portfolio,
            String schoolId,
            String manualSchoolName,
            boolean schoolCounselorVerified,
            String profilePhotoUrl
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
            String schoolLocality,
            String profilePhotoUrl
    ) {}
}
