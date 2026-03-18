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
            @RequestParam(required = false) Integer limit
    ) {
        return schoolDirectoryService.listSchools(search, limit);
    }
}
