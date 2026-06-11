package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.RecommendationDtos;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.InternshipRepository;
import com.production.ZhasIntern.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Pattern GRADE_PATTERN = Pattern.compile("\\b(7|8|9|10|11|12)\\b");

    private final ProfileRepository profileRepository;
    private final InternshipRepository internshipRepository;
    private final AiScoringClient aiScoringClient;

    @Transactional(readOnly = true)
    public List<RecommendationDtos.RecommendedInternshipDto> recommendInternships(UUID studentId) {
        UserProfile student = profileRepository.findById(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Student profile not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Student role is required");
        }

        return internshipRepository.findByStatusOrderByPublishedAtDesc(Internship.Status.PUBLISHED)
                .stream()
                .filter(this::isActive)
                .map(internship -> recommend(student, internship))
                .sorted(Comparator.comparing(
                        (RecommendationDtos.RecommendedInternshipDto dto) -> dto.recommendation().totalScore()
                ).reversed())
                .toList();
    }

    private RecommendationDtos.RecommendedInternshipDto recommend(UserProfile student, Internship internship) {
        RecommendationDtos.AiScoringRequestDto request = buildAiRequest(student, internship);
        RecommendationDtos.RecommendationScoreDto score = aiScoringClient.score(request)
                .map(aiScore -> toScore(aiScore, "AI"))
                .orElseGet(() -> toScore(ruleBasedScore(request), "RULE_BASED"));

        return new RecommendationDtos.RecommendedInternshipDto(
                internship.getId(),
                internship.getTitle(),
                internship.getCompanyName(),
                internship.getLocation(),
                internship.getType() != null ? internship.getType().name() : null,
                internship.getIsRemote(),
                internship.getShortDescription(),
                internship.getDescription(),
                internship.getDeadline(),
                internship.getPublishedAt(),
                score
        );
    }

    private boolean isActive(Internship internship) {
        return internship.getDeadline() == null || internship.getDeadline().isAfter(Instant.now());
    }

    private RecommendationDtos.AiScoringRequestDto buildAiRequest(UserProfile student, Internship internship) {
        School school = student.getSchoolEntity();
        return new RecommendationDtos.AiScoringRequestDto(
                new RecommendationDtos.StudentContext(
                        student.getId(),
                        student.getGrade(),
                        school != null ? firstNotBlank(school.getRegionRu(), school.getRegionKz()) : null,
                        firstNotBlank(student.getCity(), school != null ? school.getLocalityRu() : null),
                        student.getSchool(),
                        splitList(student.getInterests()),
                        splitList(student.getSkills()),
                        student.getPreferredFormat(),
                        student.getBio()
                ),
                new RecommendationDtos.InternshipContext(
                        internship.getId(),
                        internship.getTitle(),
                        internship.getCompanyName(),
                        internship.getLocation(),
                        internship.getType() != null ? internship.getType().name() : null,
                        internship.getIsRemote(),
                        internship.getShortDescription(),
                        internship.getDescription()
                )
        );
    }

    private RecommendationDtos.AiScoringResponseDto ruleBasedScore(RecommendationDtos.AiScoringRequestDto request) {
        RecommendationDtos.StudentContext student = request.student();
        RecommendationDtos.InternshipContext internship = request.internship();
        String internshipText = normalize(Stream.of(
                internship.title(),
                internship.companyName(),
                internship.location(),
                internship.format(),
                internship.shortDescription(),
                internship.description()
        ).filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(" ")));

        double skillMatch = overlap(student.skills(), internshipText);
        double interestMatch = overlap(student.interests(), internshipText);
        double locationMatch = locationMatch(student, internship);
        double gradeMatch = gradeMatch(student.grade(), internshipText);
        double motivationMatch = motivationMatch(student.motivation(), internshipText);

        return new RecommendationDtos.AiScoringResponseDto(
                skillMatch,
                interestMatch,
                locationMatch,
                gradeMatch,
                motivationMatch,
                "Rule-based fallback score"
        );
    }

    private RecommendationDtos.RecommendationScoreDto toScore(
            RecommendationDtos.AiScoringResponseDto raw,
            String source
    ) {
        double skill = clamp(raw.skillMatch());
        double interest = clamp(raw.interestMatch());
        double location = clamp(raw.locationMatch());
        double grade = clamp(raw.gradeMatch());
        double motivation = clamp(raw.motivationMatch());

        // Keep the final score formula in one place so AI and fallback paths stay comparable.
        double total = 0.35 * skill
                + 0.25 * interest
                + 0.20 * location
                + 0.10 * grade
                + 0.10 * motivation;

        return new RecommendationDtos.RecommendationScoreDto(
                round(total),
                round(skill),
                round(interest),
                round(location),
                round(grade),
                round(motivation),
                source,
                raw.explanation()
        );
    }

    private double overlap(List<String> tags, String internshipText) {
        Set<String> normalizedTags = tags.stream()
                .flatMap(value -> Arrays.stream(value.split("[,;\\s]+")))
                .map(this::normalize)
                .filter(value -> value.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedTags.isEmpty() || internshipText.isBlank()) {
            return 0.0;
        }

        long matches = normalizedTags.stream()
                .filter(internshipText::contains)
                .count();
        return clamp((double) matches / normalizedTags.size());
    }

    private double locationMatch(RecommendationDtos.StudentContext student, RecommendationDtos.InternshipContext internship) {
        String format = normalize(internship.format());
        String preferredFormat = normalize(student.preferredFormat());
        String location = normalize(internship.location());
        boolean remote = Boolean.TRUE.equals(internship.remote()) || "remote".equals(format) || "online".equals(format);

        if (remote && (preferredFormat.isBlank() || preferredFormat.equals("online") || preferredFormat.equals("remote"))) {
            return 1.0;
        }
        if ("hybrid".equals(format) && ("hybrid".equals(preferredFormat) || preferredFormat.isBlank())) {
            return 0.8;
        }
        if (matchesPlace(location, student.city()) || matchesPlace(location, student.region())) {
            return 1.0;
        }
        return remote ? 0.7 : 0.0;
    }

    private double gradeMatch(String studentGrade, String internshipText) {
        Integer grade = extractGrade(studentGrade);
        if (grade == null) {
            return 0.5;
        }

        Set<Integer> requiredGrades = new LinkedHashSet<>();
        Matcher matcher = GRADE_PATTERN.matcher(internshipText);
        while (matcher.find()) {
            requiredGrades.add(Integer.parseInt(matcher.group(1)));
        }

        // If an internship does not declare grade requirements, treat it as broadly suitable.
        if (requiredGrades.isEmpty()) {
            return 0.8;
        }
        return requiredGrades.contains(grade) ? 1.0 : 0.0;
    }

    private double motivationMatch(String motivation, String internshipText) {
        Set<String> motivationWords = Arrays.stream(normalize(motivation).split("\\s+"))
                .filter(word -> word.length() >= 4)
                .collect(Collectors.toSet());

        if (motivationWords.isEmpty() || internshipText.isBlank()) {
            return 0.0;
        }

        long matches = motivationWords.stream().filter(internshipText::contains).count();
        return clamp((double) matches / Math.min(motivationWords.size(), 8));
    }

    private boolean matchesPlace(String location, String place) {
        String normalizedPlace = normalize(place);
        return !location.isBlank() && !normalizedPlace.isBlank() && location.contains(normalizedPlace);
    }

    private Integer extractGrade(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = GRADE_PATTERN.matcher(raw);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private List<String> splitList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("[,;\\n]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String firstNotBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
