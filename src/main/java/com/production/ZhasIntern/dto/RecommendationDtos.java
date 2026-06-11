package com.production.ZhasIntern.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RecommendationDtos {

    public record AiScoringRequestDto(
            StudentContext student,
            InternshipContext internship
    ) {}

    public record StudentContext(
            UUID id,
            String grade,
            String region,
            String city,
            String school,
            List<String> interests,
            List<String> skills,
            String preferredFormat,
            String motivation
    ) {}

    public record InternshipContext(
            UUID id,
            String title,
            String companyName,
            String location,
            String format,
            Boolean remote,
            String shortDescription,
            String description
    ) {}

    public record AiScoringResponseDto(
            double skillMatch,
            double interestMatch,
            double locationMatch,
            double gradeMatch,
            double motivationMatch,
            String explanation
    ) {}

    public record RecommendationScoreDto(
            double totalScore,
            double skillMatch,
            double interestMatch,
            double locationMatch,
            double gradeMatch,
            double motivationMatch,
            String source,
            String explanation
    ) {}

    public record RecommendedInternshipDto(
            UUID id,
            String title,
            String companyName,
            String location,
            String type,
            Boolean isRemote,
            String shortDescription,
            String description,
            Instant deadline,
            Instant publishedAt,
            RecommendationScoreDto recommendation
    ) {}
}
