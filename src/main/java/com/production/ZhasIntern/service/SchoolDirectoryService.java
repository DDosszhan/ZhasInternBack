package com.production.ZhasIntern.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.production.ZhasIntern.dto.SchoolDtos;
import com.production.ZhasIntern.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SchoolDirectoryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 300;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${integrations.egov-schools.base-url}")
    private String baseUrl;

    @Value("${integrations.egov-schools.api-key}")
    private String apiKey;

    public SchoolDtos.SchoolListResponse listSchools(String search, Integer limit) {
        int resolvedLimit = sanitizeLimit(limit);

        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apiKey", apiKey)
                .queryParam("limit", MAX_LIMIT)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        JsonNode payload;
        try {
            payload = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_UNAVAILABLE", "Cannot load schools directory now");
        }

        if (payload == null || !payload.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_BAD_RESPONSE", "Schools directory returned invalid payload");
        }

        String normalizedSearch = normalize(search);
        List<SchoolDtos.SchoolOption> schools = new ArrayList<>();

        for (JsonNode node : payload) {
            String name = extractText(node,
                    "school", "school_name", "name", "name_rus", "name_kaz", "title", "organization_name", "naimenovanie");

            if (name == null || name.isBlank()) {
                continue;
            }

            if (normalizedSearch != null && !name.toLowerCase(Locale.ROOT).contains(normalizedSearch)) {
                continue;
            }

            String id = extractText(node, "id", "_id", "school_id", "bin");
            if (id == null || id.isBlank()) {
                id = name;
            }

            schools.add(new SchoolDtos.SchoolOption(
                    id,
                    name,
                    extractText(node, "region", "obl_name", "oblast", "region_name"),
                    extractText(node, "district", "audan", "district_name"),
                    extractText(node, "city", "qala", "city_name")
            ));

            if (schools.size() >= resolvedLimit) {
                break;
            }
        }

        schools.sort(Comparator.comparing(SchoolDtos.SchoolOption::name, String.CASE_INSENSITIVE_ORDER));
        return new SchoolDtos.SchoolListResponse(schools);
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String extractText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }
}
