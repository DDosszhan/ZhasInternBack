package com.production.ZhasIntern.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.production.ZhasIntern.dto.KatoDtos;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KatoDirectoryService {

    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 60;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${integrations.kato.base-url}")
    private String katoBaseUrl;

    @Value("${integrations.kato.api-key}")
    private String katoApiKey;

    public KatoDtos.KatoListResponse listRegions() {
        List<KatoNode> nodes = fetchAll(buildMatchQuery("Level", 1));
        return new KatoDtos.KatoListResponse(nodes.stream()
                .filter(node -> node.level() != null && node.level() == 1)
                .sorted(Comparator.comparing(
                        KatoNode::nameRu,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .map(this::toDto)
                .toList());
    }

    public KatoDtos.KatoListResponse listChildren(Integer parentId) {
        if (parentId == null) {
            return new KatoDtos.KatoListResponse(List.of());
        }
        List<KatoNode> nodes = fetchAll(buildTermQuery("Parent", parentId));
        return new KatoDtos.KatoListResponse(nodes.stream()
                .sorted(Comparator.comparing(
                        KatoNode::nameRu,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .map(this::toDto)
                .toList());
    }

    public KatoDtos.KatoListResponse listLocalities(Integer regionId, Integer districtId) {
        Integer parent = districtId != null ? districtId : regionId;
        if (parent == null) {
            return new KatoDtos.KatoListResponse(List.of());
        }

        List<KatoNode> nodes = fetchAll(buildTermQuery("Parent", parent));
        List<KatoDtos.KatoNodeOption> localities = nodes.stream()
                .filter(node -> node.level() != null && (node.level() == 5 || node.level() == 6))
                .sorted(Comparator.comparing(
                        KatoNode::nameRu,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .map(this::toDto)
                .toList();

        return new KatoDtos.KatoListResponse(localities);
    }

    private List<KatoNode> fetchAll(String sourceQuery) {
        List<KatoNode> all = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            int from = page * PAGE_SIZE;
            JsonNode pagePayload = fetchPage(sourceQuery, from, PAGE_SIZE);
            if (pagePayload == null || !pagePayload.isArray() || pagePayload.isEmpty()) {
                break;
            }

            for (JsonNode node : pagePayload) {
                Integer id = extractInt(node, "Id", "id");
                if (id == null) {
                    continue;
                }

                all.add(new KatoNode(
                        id,
                        extractInt(node, "Parent", "parent"),
                        extractInt(node, "Level", "level"),
                        extractLong(node, "Code", "code"),
                        extractText(node, "NameRus", "nameRus", "name_ru"),
                        extractText(node, "NameKaz", "nameKaz", "name_kz")
                ));
            }

            if (pagePayload.size() < PAGE_SIZE) {
                break;
            }
        }
        return all;
    }

    private JsonNode fetchPage(String sourceQuery, int from, int size) {
        validateConfig();

        final String sourceWithPaging;
        try {
            JsonNode sourceNode = objectMapper.readTree(sourceQuery);
            if (sourceNode.isObject()) {
                ((tools.jackson.databind.node.ObjectNode) sourceNode).put("from", from);
                ((tools.jackson.databind.node.ObjectNode) sourceNode).put("size", size);
            }
            sourceWithPaging = objectMapper.writeValueAsString(sourceNode);
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "KATO_QUERY_ERROR", "Cannot build KATO query");
        }

        final String url;
        try {
            url = UriComponentsBuilder.fromUriString(katoBaseUrl)
                    .queryParam("apiKey", katoApiKey)
                    .queryParam("source", sourceWithPaging)
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "KATO_CONFIG_ERROR", "KATO directory is not configured properly");
        }

        final JsonNode payload;
        try {
            payload = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KATO_UNAVAILABLE", "Cannot load KATO directory now");
        }

        JsonNode recordsNode = resolveRecordsNode(payload);
        if (recordsNode == null || !recordsNode.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "KATO_BAD_RESPONSE", "KATO returned invalid payload");
        }
        return recordsNode;
    }

    private String buildMatchQuery(String field, int value) {
        Map<String, Object> root = new HashMap<>();
        root.put("query", Map.of("match", Map.of(field, value)));
        return writeQuery(root);
    }

    private String buildTermQuery(String field, int value) {
        Map<String, Object> root = new HashMap<>();
        root.put("query", Map.of("term", Map.of(field, value)));
        return writeQuery(root);
    }

    private String writeQuery(Map<String, Object> root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JacksonException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "KATO_QUERY_ERROR", "Cannot build KATO query");
        }
    }

    private void validateConfig() {
        if (katoBaseUrl == null || katoBaseUrl.isBlank() || katoApiKey == null || katoApiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "KATO_CONFIG_ERROR", "KATO directory is not configured properly");
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

    private Integer extractInt(JsonNode node, String... keys) {
        String value = extractText(node, keys);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long extractLong(JsonNode node, String... keys) {
        String value = extractText(node, keys);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private KatoDtos.KatoNodeOption toDto(KatoNode node) {
        return new KatoDtos.KatoNodeOption(
                node.id(),
                node.parent(),
                node.level(),
                node.code(),
                node.nameRu(),
                node.nameKz()
        );
    }

    private record KatoNode(
            Integer id,
            Integer parent,
            Integer level,
            Long code,
            String nameRu,
            String nameKz
    ) {
    }
}
