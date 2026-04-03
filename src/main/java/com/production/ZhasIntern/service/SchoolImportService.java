package com.production.ZhasIntern.service;

import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolImportService {

    private static final String SOURCE = "onirler_oblystar_kalalar_boi4";
    private static final String SOURCE_VERSION = "v3";
    private static final int PAGE_SIZE = 100;

    private static final Pattern MULTI_SPACES = Pattern.compile("\\s+");
    private static final Pattern QUOTES = Pattern.compile("[\"'`«»“”„]");
    private static final Pattern SERVICE_WORDS = Pattern.compile("\\b(гу|кгу|кгкп|гккп|гуп|кмму|коммунальное|государственное|учреждение)\\b");

    private final EgovSchoolClient egovSchoolClient;
    private final SchoolRepository schoolRepository;

    @Transactional
    public SchoolImportResult importFromEgovV3() {
        int from = 0;
        int total = 0;
        int created = 0;
        int updated = 0;

        while (true) {
            JsonNode page = egovSchoolClient.fetchSchoolsPage(from, PAGE_SIZE);
            if (page == null || !page.isArray() || page.isEmpty()) {
                break;
            }

            for (JsonNode node : page) {
                String externalId = text(node, "id");
                if (externalId == null || externalId.isBlank()) {
                    continue;
                }

                School school = schoolRepository
                        .findBySourceAndSourceVersionAndExternalId(SOURCE, SOURCE_VERSION, externalId)
                        .orElseGet(() -> {
                            School createdEntity = new School();
                            createdEntity.setId(UUID.randomUUID());
                            createdEntity.setSource(SOURCE);
                            createdEntity.setSourceVersion(SOURCE_VERSION);
                            createdEntity.setExternalId(externalId);
                            return createdEntity;
                        });

                boolean exists = school.getCreatedAt() != null;
                mapFields(school, node);
                schoolRepository.save(school);

                total++;
                if (exists) {
                    updated++;
                } else {
                    created++;
                }
            }

            if (page.size() < PAGE_SIZE) {
                break;
            }
            from += PAGE_SIZE;
        }

        log.info("Schools import finished: total={}, created={}, updated={}", total, created, updated);
        return new SchoolImportResult(total, created, updated);
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
