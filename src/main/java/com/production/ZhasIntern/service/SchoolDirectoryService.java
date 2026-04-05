package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.SchoolDtos;
import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SchoolDirectoryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 300;

    private final SchoolRepository schoolRepository;

    public SchoolDtos.SchoolListResponse listSchools(
            String search,
            String region,
            String district,
            String locality,
            Integer limit
    ) {
        int resolvedLimit = sanitizeLimit(limit);
        String normalizedSearch = normalize(search);
        String normalizedRegion = normalize(region);
        String normalizedDistrict = normalize(district);
        String normalizedLocality = normalize(locality);

        List<School> matchedSchools = normalizedSearch == null
                ? schoolRepository.findByFilters(
                        normalizedRegion,
                        normalizedDistrict,
                        normalizedLocality
                )
                : schoolRepository.searchByFilters(
                        normalizedSearch,
                        normalizedRegion,
                        normalizedDistrict,
                        normalizedLocality
                );

        List<SchoolDtos.SchoolOption> schools = matchedSchools.stream()
                .limit(resolvedLimit)
                .map(this::toOption)
                .toList();

        return new SchoolDtos.SchoolListResponse(schools);
    }

    public SchoolDtos.SchoolFilterOptionsResponse listFilterOptions(String region, String district) {
        String normalizedRegion = normalize(region);
        String normalizedDistrict = normalize(district);

        return new SchoolDtos.SchoolFilterOptionsResponse(
                schoolRepository.findDistinctRegionsRu(),
                schoolRepository.findDistinctDistrictsRu(normalizedRegion),
                schoolRepository.findDistinctLocalitiesRu(normalizedRegion, normalizedDistrict)
        );
    }

    public SchoolDtos.RegionListResponse listRegions() {
        return new SchoolDtos.RegionListResponse(schoolRepository.findDistinctRegionsRu());
    }

    public SchoolDtos.DistrictListResponse listDistricts(String region) {
        return new SchoolDtos.DistrictListResponse(schoolRepository.findDistinctDistrictsRu(normalize(region)));
    }

    public SchoolDtos.AreaListResponse listAreas(String region) {
        return new SchoolDtos.AreaListResponse(schoolRepository.findDistinctDistrictsRu(normalize(region)));
    }

    public SchoolDtos.LocalityListResponse listLocalities(String region, String district) {
        return new SchoolDtos.LocalityListResponse(
                schoolRepository.findDistinctLocalitiesRu(normalize(region), normalize(district))
        );
    }

    public SchoolDtos.SettlementListResponse listSettlements(String region, String area) {
        return new SchoolDtos.SettlementListResponse(
                schoolRepository.findDistinctLocalitiesRu(normalize(region), normalize(area))
        );
    }

    private SchoolDtos.SchoolOption toOption(School school) {
        return new SchoolDtos.SchoolOption(
                school.getId().toString(),
                firstNonBlank(school.getSchoolNameRu(), school.getSchoolNameKz()),
                firstNonBlank(school.getRegionRu(), school.getRegionKz()),
                firstNonBlank(school.getDistrictRu(), school.getDistrictKz()),
                firstNonBlank(school.getLocalityRu(), school.getLocalityKz())
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
