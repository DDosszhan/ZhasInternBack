package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.RecommendationDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient implements AiScoringClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Override
    public Optional<RecommendationDtos.AiScoringResponseDto> score(RecommendationDtos.AiScoringRequestDto request) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .pathSegment("models", model + ":generateContent")
                    .queryParam("key", apiKey)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", buildPrompt(request)))
                    )),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "responseMimeType", "application/json"
                    )
            );

            JsonNode response = restTemplate.postForObject(url, payload, JsonNode.class);
            String json = extractResponseText(response);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(parseScoringJson(json));
        } catch (RestClientException | JacksonException | IllegalArgumentException ex) {
            log.warn("Gemini scoring unavailable, falling back to rule-based recommendations: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String buildPrompt(RecommendationDtos.AiScoringRequestDto request) throws JacksonException {
        String context = objectMapper.writeValueAsString(request);
        return """
                You score how well an internship fits a school student.
                Return strictly valid JSON and no markdown.
                Each score must be a number from 0.0 to 1.0.
                JSON schema:
                {
                  "skillMatch": 0.0,
                  "interestMatch": 0.0,
                  "locationMatch": 0.0,
                  "gradeMatch": 0.0,
                  "motivationMatch": 0.0,
                  "explanation": "short explanation"
                }

                Context:
                %s
                """.formatted(context);
    }

    private String extractResponseText(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode candidates = response.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        JsonNode content = candidates.get(0).get("content");
        if (content == null || content.isNull()) {
            return null;
        }
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            return null;
        }
        JsonNode text = parts.get(0).get("text");
        return text == null || text.isNull() ? null : stripJsonFence(text.asText());
    }

    private String stripJsonFence(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        return normalized.trim();
    }

    private RecommendationDtos.AiScoringResponseDto parseScoringJson(String json) throws JacksonException {
        JsonNode root = objectMapper.readTree(json);
        return new RecommendationDtos.AiScoringResponseDto(
                clamp(number(root, "skillMatch")),
                clamp(number(root, "interestMatch")),
                clamp(number(root, "locationMatch")),
                clamp(number(root, "gradeMatch")),
                clamp(number(root, "motivationMatch")),
                text(root, "explanation")
        );
    }

    private double number(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
