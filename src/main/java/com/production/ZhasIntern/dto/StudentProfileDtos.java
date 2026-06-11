package com.production.ZhasIntern.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class StudentProfileDtos {

    public record StudentProfileResponse(
            UUID id,
            String fullName,
            String bio,
            String phone,
            String school,
            String grade,
            String city,
            String portfolio,
            String email,
            String profilePhotoUrl,
            List<String> skills,
            List<String> interests,
            String preferredFormat,
            Instant createdAt
    ) {}
}
