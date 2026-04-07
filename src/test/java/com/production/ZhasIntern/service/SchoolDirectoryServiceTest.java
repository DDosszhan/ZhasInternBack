package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.SchoolDtos;
import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchoolDirectoryServiceTest {

    private SchoolRepository schoolRepository;
    private SchoolDirectoryService service;

    @BeforeEach
    void setUp() {
        schoolRepository = mock(SchoolRepository.class);
        service = new SchoolDirectoryService(schoolRepository);
    }

    @Test
    void listSchoolsRespectsLimit() {
        when(schoolRepository.findByFilters(null, null, null))
                .thenReturn(List.of(school("A"), school("B")));

        SchoolDtos.SchoolListResponse response = service.listSchools(null, null, null, null, 1);

        assertEquals(1, response.schools().size());
        assertEquals("A", response.schools().get(0).name());
    }

    @Test
    void listSchoolsUsesSearchWhenSearchProvidedAndNormalizesFilters() {
        when(schoolRepository.searchByFilters("lyceum", "astana", "saryarka", "astana"))
                .thenReturn(List.of(school("School RU", "Astana", "Saryarka", "Astana", null)));

        SchoolDtos.SchoolListResponse response = service.listSchools("  Lyceum  ", " ASTANA ", " Saryarka ", " Astana ", 20);

        assertEquals(1, response.schools().size());
        verify(schoolRepository).searchByFilters("lyceum", "astana", "saryarka", "astana");
    }

    @Test
    void listSchoolsFallsBackToKzNameWhenRuNameMissing() {
        when(schoolRepository.findByFilters(null, null, null))
                .thenReturn(List.of(school(null, "Region", "District", "Locality", "KZ Name")));

        SchoolDtos.SchoolListResponse response = service.listSchools(null, null, null, null, 20);

        assertEquals("KZ Name", response.schools().get(0).name());
    }

    @Test
    void listSchoolsNormalizesLimitBoundaries() {
        when(schoolRepository.findByFilters(null, null, null))
                .thenReturn(List.of(school("A"), school("B"), school("C")));

        SchoolDtos.SchoolListResponse minLimited = service.listSchools(null, null, null, null, 0);
        SchoolDtos.SchoolListResponse maxLimited = service.listSchools(null, null, null, null, 999);

        assertEquals(1, minLimited.schools().size());
        assertEquals(3, maxLimited.schools().size());
    }

    @Test
    void listFilterOptionsNormalizesRegionAndDistrict() {
        when(schoolRepository.findDistinctRegionsRu()).thenReturn(List.of("Алматы", "Астана"));
        when(schoolRepository.findDistinctDistrictsRu(eq("astana"))).thenReturn(List.of("Saryarka"));
        when(schoolRepository.findDistinctLocalitiesRu(eq("astana"), eq("saryarka"))).thenReturn(List.of("Astana"));

        SchoolDtos.SchoolFilterOptionsResponse response = service.listFilterOptions(" Astana ", " Saryarka ");

        assertEquals(List.of("Алматы", "Астана"), response.regions());
        assertEquals(List.of("Saryarka"), response.districts());
        assertEquals(List.of("Astana"), response.localities());
    }

    private School school(String name) {
        return school(name, "Регион", "Район", "Город", null);
    }

    private School school(String name, String region, String district, String locality, String nameKz) {
        School school = new School();
        school.setId(UUID.randomUUID());
        school.setSchoolNameRu(name);
        school.setSchoolNameKz(nameKz);
        school.setRegionRu(region);
        school.setDistrictRu(district);
        school.setLocalityRu(locality);
        school.setNormalizedName(name == null ? null : name.toLowerCase(Locale.ROOT));
        return school;
    }
}
