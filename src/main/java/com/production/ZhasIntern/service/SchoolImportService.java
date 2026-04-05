package com.production.ZhasIntern.service;

import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolImportService {

    private static final String SOURCE = "onirler_oblystar_kalalar_boi4";
    private static final String SOURCE_VERSION = "v3";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 500;

    private static final Pattern MULTI_SPACES = Pattern.compile("\\s+");
    private static final Pattern QUOTES = Pattern.compile("[\"'`«»“”„]");
    private static final Pattern SERVICE_WORDS = Pattern.compile("\\b(гу|кгу|кгкп|гккп|гуп|кмму|коммунальное|государственное|учреждение)\\b");

    private final EgovSchoolClient egovSchoolClient;
    private final SchoolRepository schoolRepository;

    public SchoolImportResult importFromEgovV3(int startFrom) {
        if (startFrom < 0) {
            throw new IllegalArgumentException("Import offset cannot be negative");
        }

        int from = startFrom;
        int pageNumber = 0;
        int total = 0;
        int created = 0;
        int updated = 0;
        Set<String> seenPageSignatures = new HashSet<>();

        log.info("Schools import started: startFrom={}, pageSize={}", startFrom, PAGE_SIZE);

        while (pageNumber < MAX_PAGES) {
            JsonNode page = egovSchoolClient.fetchSchoolsPage(from, PAGE_SIZE);
            if (page == null || !page.isArray() || page.isEmpty()) {
                log.info("Schools import stopped: empty page returned for from={}", from);
                break;
            }

            String pageSignature = buildPageSignature(page);
            if (!seenPageSignatures.add(pageSignature)) {
                log.warn("Schools import stopped: provider returned a repeated page for from={}, pageNumber={}, signature={}",
                        from, pageNumber, pageSignature);
                break;
            }

            List<JsonNode> validNodes = new ArrayList<>();
            Set<String> externalIds = new LinkedHashSet<>();
            for (JsonNode node : page) {
                String externalId = text(node, "id");
                if (externalId == null || externalId.isBlank()) {
                    continue;
                }
                validNodes.add(node);
                externalIds.add(externalId);
            }

            Map<String, School> existingByExternalId = schoolRepository
                    .findAllBySourceAndSourceVersionAndExternalIdIn(SOURCE, SOURCE_VERSION, externalIds)
                    .stream()
                    .collect(Collectors.toMap(School::getExternalId, Function.identity()));

            int processedOnPage = 0;
            List<School> schoolsToSave = new ArrayList<>(validNodes.size());
            for (JsonNode node : validNodes) {
                String externalId = text(node, "id");

                School school = existingByExternalId.get(externalId);
                boolean exists = school != null;
                if (!exists) {
                    school = existingByExternalId.computeIfAbsent(externalId, key -> {
                            School createdEntity = new School();
                            createdEntity.setId(UUID.randomUUID());
                            createdEntity.setSource(SOURCE);
                            createdEntity.setSourceVersion(SOURCE_VERSION);
                            createdEntity.setExternalId(key);
                            return createdEntity;
                        });
                }
                mapFields(school, node);
                schoolsToSave.add(school);

                total++;
                processedOnPage++;
                if (exists) {
                    updated++;
                } else {
                    created++;
                }
            }

            schoolRepository.saveAll(schoolsToSave);

            log.info("Schools import page processed: pageNumber={}, from={}, received={}, persisted={}",
                    pageNumber, from, page.size(), processedOnPage);

            if (page.size() < PAGE_SIZE) {
                log.info("Schools import stopped: last partial page received for from={}, size={}", from, page.size());
                break;
            }
            from += PAGE_SIZE;
            pageNumber++;
        }

        if (pageNumber >= MAX_PAGES) {
            log.warn("Schools import stopped after reaching MAX_PAGES={} to avoid infinite looping", MAX_PAGES);
        }

        log.info("Schools import finished: startFrom={}, total={}, created={}, updated={}",
                startFrom, total, created, updated);
        log.info("All schools import pages completed successfully from offset {}", startFrom);
        return new SchoolImportResult(total, created, updated);
    }

    private String buildPageSignature(JsonNode page) {
        StringBuilder signature = new StringBuilder();
        for (JsonNode node : page) {
            String externalId = text(node, "id");
            if (externalId != null) {
                signature.append(externalId).append('|');
            }
        }
        return signature.toString();
    }

    private void mapFields(School school, JsonNode node) {
        school.setRegionRu(text(node, "region"));
        school.setRegionKz(text(node, "region_kz"));
        school.setDistrictRu(text(node, "area"));
        school.setDistrictKz(text(node, "area_kz"));
        school.setLocalityRu(text(node, "sity"));
        school.setLocalityKz(text(node, "sity_kz"));
        school.setSchoolNameRu(text(node, "name"));
        school.setSchoolNameKz(text(node, "name_kz"));
        school.setSchoolTypeRu(text(node, "type_of_organization"));
        school.setSchoolTypeKz(text(node, "type_of_organization_kz"));
        school.setNormalizedName(normalizeSchoolName(school.getSchoolNameRu()));
        school.setActive(true);
        school.setRawJson(node.toString());
    }

    private String normalizeSchoolName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        normalized = QUOTES.matcher(normalized).replaceAll(" ");
        normalized = SERVICE_WORDS.matcher(normalized).replaceAll(" ");
        normalized = MULTI_SPACES.matcher(normalized).replaceAll(" ").trim();

        return normalized.isBlank() ? null : normalized;
    }

    private String text(JsonNode node, String field) {
        JsonNode raw = node.get(field);
        if (raw == null || raw.isNull()) {
            return null;
        }
        String value = raw.asText();
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record SchoolImportResult(int total, int created, int updated) {
    }
}
