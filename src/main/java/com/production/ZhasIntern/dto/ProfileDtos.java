package com.production.ZhasIntern.dto;

public class ProfileDtos {

    public record UpdateRoleRequest(String role) {}

    public record UpdateStudentDetailsRequest(
            String fullName,
            String bio,
            String school,
            String grade,
            String city,
            String portfolio
    ) {}

    public record ProfileRoleResponse(
            String userId,
            String role
    ) {}

    public record StudentDetailsResponse(
            String userId,
            String fullName,
            String bio,
            String school,
            String grade,
            String city,
            String portfolio
    ) {}
}
