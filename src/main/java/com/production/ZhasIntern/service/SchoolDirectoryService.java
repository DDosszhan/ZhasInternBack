package com.production.ZhasIntern.service;

import tools.jackson.databind.JsonNode;
import com.production.ZhasIntern.dto.SchoolDtos;
import com.production.ZhasIntern.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SchoolDirectoryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 300;
    private static final int FETCH_BATCH_SIZE = 1000;
    private static final int MAX_FETCH_PAGES = 50;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${integrations.egov-schools.base-url}")
    private String baseUrl;

    @Value("${integrations.egov-schools.api-key}")
    private String apiKey;

    public SchoolDtos.SchoolListResponse listSchools(
            String search,
            String region,
            String district,
            String locality,
            Integer limit
    ) {
        int resolvedLimit = sanitizeLimit(limit);
        List<SchoolRecord> records = loadRecords();

        String normalizedSearch = normalize(search);
        String normalizedRegion = normalize(region);
        String normalizedDistrict = normalize(district);
        String normalizedLocality = normalize(locality);

        List<SchoolDtos.SchoolOption> schools = records.stream()
                .filter(record -> matchesEquals(normalizedRegion, record.normalizedRegion()))
                .filter(record -> matchesEquals(normalizedDistrict, record.normalizedArea()))
                .filter(record -> matchesEquals(normalizedLocality, record.normalizedLocality()))
                .filter(record -> matchesSearch(normalizedSearch, record))
                .sorted(Comparator.comparing(SchoolRecord::name, String.CASE_INSENSITIVE_ORDER))
                .limit(resolvedLimit)
                .map(record -> new SchoolDtos.SchoolOption(
                        record.id(),
                        record.name(),
                        record.region(),
                        record.district(),
                        record.locality()
                ))
                .toList();

        return new SchoolDtos.SchoolListResponse(schools);
    }

    public SchoolDtos.SchoolFilterOptionsResponse listFilterOptions(String region, String district) {
        List<SchoolRecord> records = loadRecords();
        String normalizedRegion = normalize(region);
        String normalizedDistrict = normalize(district);

        Set<String> regions = new LinkedHashSet<>();
        Set<String> districts = new LinkedHashSet<>();
        Set<String> localities = new LinkedHashSet<>();

        for (SchoolRecord record : records) {
            if (record.region() != null) {
                regions.add(record.region());
            }

            if (matchesEquals(normalizedRegion, record.normalizedRegion()) && record.district() != null) {
                districts.add(record.district());
            }

            if (matchesEquals(normalizedRegion, record.normalizedRegion())
                    && matchesEquals(normalizedDistrict, record.normalizedArea())
                    && record.locality() != null) {
                localities.add(record.locality());
            }

        }
        return new SchoolDtos.SchoolFilterOptionsResponse(
                regions.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                districts.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                localities.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()
        );
    }


    public SchoolDtos.RegionListResponse listRegions() {
        List<SchoolRecord> records = loadRecords();
        List<String> regions = records.stream()
                .map(SchoolRecord::region)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new SchoolDtos.RegionListResponse(regions);
    }

    public SchoolDtos.DistrictListResponse listDistricts(String region) {
        List<SchoolRecord> records = loadRecords();
        String normalizedRegion = normalize(region);

        List<String> districts = records.stream()
                .filter(record -> matchesEquals(normalizedRegion, record.normalizedRegion()))
                .map(SchoolRecord::district)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new SchoolDtos.DistrictListResponse(districts);
    }

    public SchoolDtos.LocalityListResponse listLocalities(String region, String district) {
        List<SchoolRecord> records = loadRecords();
        String normalizedRegion = normalize(region);
        String normalizedDistrict = normalize(district);

        List<String> localities = records.stream()
                .filter(record -> matchesEquals(normalizedRegion, record.normalizedRegion()))
                .filter(record -> matchesEquals(normalizedDistrict, record.normalizedArea()))
                .map(SchoolRecord::locality)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new SchoolDtos.LocalityListResponse(localities);
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
        validateConfig();

        List<JsonNode> allItems = new ArrayList<>();
        int offset = 0;
        String previousPageSignature = null;
        for (int page = 0; page < MAX_FETCH_PAGES; page++) {
            JsonNode pageItems = loadPayloadPage(offset, FETCH_BATCH_SIZE);
            if (pageItems == null || !pageItems.isArray() || pageItems.isEmpty()) {
                break;
            }

            String currentPageSignature = buildPageSignature(pageItems);
            if (currentPageSignature.equals(previousPageSignature)) {
                break;
            }
            previousPageSignature = currentPageSignature;

            pageItems.forEach(allItems::add);

            if (pageItems.size() < FETCH_BATCH_SIZE) {
                break;
            }
            offset += FETCH_BATCH_SIZE;
        }

        if (allItems.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_BAD_RESPONSE", "Schools directory returned invalid payload");
        }

        return new tools.jackson.databind.node.ArrayNode(
                tools.jackson.databind.node.JsonNodeFactory.instance,
                allItems
        );
    }

    private JsonNode loadPayloadPage(int offset, int limit) {
        final String url;
        try {
            url = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("apiKey", apiKey)
                    .queryParam("limit", limit)
                    .queryParam("offset", offset)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            log.error("Invalid EGOV schools URL configuration: {}", baseUrl, ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SCHOOL_DIRECTORY_CONFIG_ERROR", "Schools directory is not configured properly");
        }

        final JsonNode payload;
        try {
            payload = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_UNAVAILABLE", "Cannot load schools directory now");
        }

        JsonNode recordsNode = resolveRecordsNode(payload);
        if (recordsNode == null || !recordsNode.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_BAD_RESPONSE", "Schools directory returned invalid payload");
        }

        return recordsNode;
    }

    private String buildPageSignature(JsonNode pageItems) {
        JsonNode first = pageItems.get(0);
        JsonNode last = pageItems.get(pageItems.size() - 1);
        return first.toString() + "|" + last.toString() + "|" + pageItems.size();
    }

    private void validateConfig() {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SCHOOL_DIRECTORY_CONFIG_ERROR", "Schools directory is not configured properly");
        }
    }

    private JsonNode resolveRecordsNode(JsonNode payload) {
        if (payload == null) {
            return null;
        }
        if (payload.isArray()) {
            return payload;
        }

        // EGOV integrations sometimes wrap items in a container object.
        JsonNode results = payload.get("results");
        if (results != null && results.isArray()) {
            return results;
        }

        JsonNode data = payload.get("data");
        if (data != null && data.isArray()) {
            return data;
        }

        return null;
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
            String district,
            String locality,
            String normalizedName,
            String normalizedRegion,
            String normalizedArea,
            String normalizedLocality
    ) {
    }
}
