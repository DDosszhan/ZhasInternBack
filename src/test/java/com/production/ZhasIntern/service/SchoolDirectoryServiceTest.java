package com.production.ZhasIntern.service;

import com.production.ZhasIntern.entity.School;
import com.production.ZhasIntern.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        when(schoolRepository.search(any(), any(), any(), any())).thenReturn(List.of(school("A"), school("B")));

        var response = service.listSchools(null, null, null, null, 1);

        assertEquals(1, response.schools().size());
    }

    @Test
    void listRegionsDelegatesToRepository() {
        when(schoolRepository.findDistinctRegionsRu()).thenReturn(List.of("Алматы", "Астана"));

        var response = service.listRegions();

        assertEquals(2, response.regions().size());
        assertEquals("Алматы", response.regions().get(0));
    }

    private School school(String name) {
        School school = new School();
        school.setId(UUID.randomUUID());
        school.setSchoolNameRu(name);
        school.setRegionRu("Регион");
        school.setDistrictRu("Район");
        school.setLocalityRu("Город");
        return school;
    }
}
