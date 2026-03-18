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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    public SchoolDtos.SchoolListResponse listSchools(
            String search,
            String region,
            String area,
            String locality,
            Integer limit
    ) {
        int resolvedLimit = sanitizeLimit(limit);
        List<SchoolRecord> records = loadRecords();

        String normalizedSearch = normalize(search);
        String normalizedRegion = normalize(region);
        String normalizedArea = normalize(area);
        String normalizedLocality = normalize(locality);

        List<SchoolDtos.SchoolOption> schools = records.stream()
                .filter(record -> matchesEquals(normalizedRegion, record.normalizedRegion()))
                .filter(record -> matchesEquals(normalizedArea, record.normalizedArea()))
                .filter(record -> matchesEquals(normalizedLocality, record.normalizedLocality()))
                .filter(record -> matchesSearch(normalizedSearch, record))
                .sorted(Comparator.comparing(SchoolRecord::name, String.CASE_INSENSITIVE_ORDER))
                .limit(resolvedLimit)
                .map(record -> new SchoolDtos.SchoolOption(
                        record.id(),
                        record.name(),
                        record.region(),
                        record.area(),
                        record.locality()
                ))
                .toList();

        return new SchoolDtos.SchoolListResponse(schools);
    }

    public SchoolDtos.SchoolFilterOptionsResponse listFilterOptions(String region, String area) {
        List<SchoolRecord> records = loadRecords();
        String normalizedRegion = normalize(region);
        String normalizedArea = normalize(area);

        Set<String> regions = new LinkedHashSet<>();
        Set<String> areas = new LinkedHashSet<>();
        Set<String> localities = new LinkedHashSet<>();

        for (SchoolRecord record : records) {
            if (record.region() != null) {
                regions.add(record.region());
            }

            if (matchesEquals(normalizedRegion, record.normalizedRegion()) && record.area() != null) {
                areas.add(record.area());
            }
        }

            if (matchesEquals(normalizedRegion, record.normalizedRegion())
                    && matchesEquals(normalizedArea, record.normalizedArea())
                    && record.locality() != null) {
                localities.add(record.locality());
            }
        }

        return new SchoolDtos.SchoolFilterOptionsResponse(
                regions.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                areas.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                localities.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()
        );
    }

    private List<SchoolRecord> loadRecords() {
        JsonNode payload = loadPayload();
        List<SchoolRecord> records = new ArrayList<>();

        for (JsonNode node : payload) {
            String name = extractText(node, "name", "name_kz", "school", "school_name", "name_rus", "name_kaz", "title", "organization_name", "naimenovanie");
            if (name == null || name.isBlank()) {
                continue;
            }

            String region = extractText(node, "region", "region_kz", "obl_name", "oblast", "region_name");
            String schoolArea = extractText(node, "area", "area_kz", "district", "audan", "district_name");
            String schoolLocality = extractText(node, "sity", "sity_kz", "city", "qala", "city_name");
            String id = extractText(node, "id", "_id", "school_id", "bin");
            if (id == null || id.isBlank()) {
                id = name;
            }

            records.add(new SchoolRecord(
                    id,
                    name,
                    region,
                    schoolArea,
                    schoolLocality,
                    normalize(name),
                    normalize(region),
                    normalize(schoolArea),
                    normalize(schoolLocality)
            ));
        }

        return records;
    }

    private JsonNode loadPayload() {
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

        return payload;
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

    private boolean matchesEquals(String requestedValue, String actualValue) {
        if (requestedValue == null) {
            return true;
        }
        if (actualValue == null) {
            return false;
        }
        return actualValue.equals(requestedValue);
    }

    private boolean matchesSearch(String normalizedSearch, SchoolRecord record) {
        if (normalizedSearch == null) {
            return true;
        }
        return contains(record.normalizedName(), normalizedSearch)
                || contains(record.normalizedRegion(), normalizedSearch)
                || contains(record.normalizedArea(), normalizedSearch)
                || contains(record.normalizedLocality(), normalizedSearch);
    }

    private boolean contains(String value, String fragment) {
        return value != null && value.contains(fragment);
    }

    private String extractText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text;
                try {
                    text = value.asText();
                } catch (Exception ex) {
                    continue;
                }
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private record SchoolRecord(
            String id,
            String name,
            String region,
            String area,
            String locality,
            String normalizedName,
            String normalizedRegion,
            String normalizedArea,
            String normalizedLocality
    ) {
    }
}
