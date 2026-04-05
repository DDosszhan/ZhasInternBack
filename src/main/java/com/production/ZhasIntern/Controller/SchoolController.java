package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.SchoolDtos;
import com.production.ZhasIntern.service.SchoolDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolDirectoryService schoolDirectoryService;

    @GetMapping
    public SchoolDtos.SchoolListResponse listSchools(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String locality,
            @RequestParam(required = false) Integer limit
    ) {
        String resolvedDistrict = district != null ? district : area;
        return schoolDirectoryService.listSchools(search, region, resolvedDistrict, locality, limit);
    }

    @GetMapping("/filters")
    public SchoolDtos.SchoolFilterOptionsResponse listFilters(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String area
    ) {
        String resolvedDistrict = district != null ? district : area;
        return schoolDirectoryService.listFilterOptions(region, resolvedDistrict);
    }

    @GetMapping("/regions")
    public SchoolDtos.RegionListResponse listRegions() {
        return schoolDirectoryService.listRegions();
    }

    @GetMapping("/districts")
    public SchoolDtos.DistrictListResponse listDistricts(
            @RequestParam(required = false) String region
    ) {
        return schoolDirectoryService.listDistricts(region);
    }

    @GetMapping("/areas")
    public SchoolDtos.AreaListResponse listAreas(
            @RequestParam(required = false) String region
    ) {
        return schoolDirectoryService.listAreas(region);
    }

    @GetMapping("/localities")
    public SchoolDtos.LocalityListResponse listLocalities(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String area
    ) {
        String resolvedDistrict = district != null ? district : area;
        return schoolDirectoryService.listLocalities(region, resolvedDistrict);
    }

    @GetMapping("/settlements")
    public SchoolDtos.SettlementListResponse listSettlements(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String area
    ) {
        String resolvedArea = area != null ? area : district;
        return schoolDirectoryService.listSettlements(region, resolvedArea);
    }
}
