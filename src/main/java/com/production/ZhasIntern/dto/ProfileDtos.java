package com.production.ZhasIntern.dto;

public class ProfileDtos {

    public record UpdateRoleRequest(String role) {}

    public record ProfileRoleResponse(
            String userId,
            String role
    ) {}
}