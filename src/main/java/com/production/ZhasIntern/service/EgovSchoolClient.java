package com.production.ZhasIntern.service;

import com.production.ZhasIntern.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EgovSchoolClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${integrations.egov-schools.base-url}")
    private String baseUrl;

    @Value("${integrations.egov-schools.api-key}")
    private String apiKey;

    public JsonNode fetchSchoolsPage(int from, int size) {
        validateConfig();

        final String source;
        try {
            source = objectMapper.writeValueAsString(Map.of("size", size, "from", from));
        } catch (JacksonException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EGOV_SOURCE_BUILD_FAILED", "Cannot build source query");
        }

        final String url;
        try {
            String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String encodedSource = URLEncoder.encode(source, StandardCharsets.UTF_8);
            String separator = baseUrl.contains("?") ? "&" : "?";
            url = baseUrl + separator + "apiKey=" + encodedApiKey + "&source=" + encodedSource;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EGOV_CONFIG_ERROR", "Schools provider configuration is invalid");
        }

        final JsonNode payload;
        try {
            payload = restTemplate.getForObject(URI.create(url), JsonNode.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_UNAVAILABLE", "Cannot load schools from EGOV now");
        }

        JsonNode records = resolveRecordsNode(payload);
        if (records == null || !records.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "EGOV_BAD_RESPONSE", "Invalid schools payload from EGOV");
        }

        return records;
    }

    private void validateConfig() {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EGOV_CONFIG_ERROR", "Schools provider is not configured");
        }
    }

    private JsonNode resolveRecordsNode(JsonNode payload) {
        if (payload == null) {
            return null;
        }
        if (payload.isArray()) {
            return payload;
        }
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
}
