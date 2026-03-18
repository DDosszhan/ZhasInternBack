package com.production.ZhasIntern.dto;

import java.util.List;

public class SchoolDtos {

    public record SchoolOption(
            String id,
            String name,
            String region,
            String district,
            String city
    ) {
    }

    public record SchoolListResponse(List<SchoolOption> schools) {
    }
}
