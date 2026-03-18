package com.production.ZhasIntern.service;

import com.production.ZhasIntern.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchoolDirectoryServiceTest {

    private SchoolDirectoryService service;

    @BeforeEach
    void setUp() {
        service = new SchoolDirectoryService();
    }

    @Test
    void listSchoolsThrowsControlledErrorWhenDirectoryConfigMissing() {
        ReflectionTestUtils.setField(service, "baseUrl", "");
        ReflectionTestUtils.setField(service, "apiKey", "");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listSchools(null, null, null, null, 10));

        assertEquals("SCHOOL_DIRECTORY_CONFIG_ERROR", ex.getCode());
        assertEquals(503, ex.getStatus().value());
    }

    @Test
    void listSchoolsThrowsControlledErrorWhenBaseUrlMalformed() {
        ReflectionTestUtils.setField(service, "baseUrl", "ht!tp:// badly formed");
        ReflectionTestUtils.setField(service, "apiKey", "token");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listSchools(null, null, null, null, 10));

        assertEquals("SCHOOL_DIRECTORY_CONFIG_ERROR", ex.getCode());
        assertEquals(503, ex.getStatus().value());
    }
}
