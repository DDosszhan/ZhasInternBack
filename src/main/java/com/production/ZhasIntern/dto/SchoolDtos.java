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
        public SchoolListResponse {
            schools = schools == null ? List.of() : schools;
        }
    }

    public record SchoolFilterOptionsResponse(
            List<String> regions,
            List<String> districts,
            List<String> localities
    ) {
        public SchoolFilterOptionsResponse {
            regions = regions == null ? List.of() : regions;
            districts = districts == null ? List.of() : districts;
            localities = localities == null ? List.of() : localities;
        }
    }

    public record RegionListResponse(List<String> regions) {
        public RegionListResponse {
            regions = regions == null ? List.of() : regions;
        }
    }

    public record DistrictListResponse(List<String> districts) {
        public DistrictListResponse {
            districts = districts == null ? List.of() : districts;
        }
    }

    public record AreaListResponse(List<String> areas) {
        public AreaListResponse {
            areas = areas == null ? List.of() : areas;
        }
    }

    public record LocalityListResponse(List<String> localities) {
        public LocalityListResponse {
            localities = localities == null ? List.of() : localities;
        }
    }

    public record SettlementListResponse(List<String> settlements) {
        public SettlementListResponse {
            settlements = settlements == null ? List.of() : settlements;
        }
    }
}
