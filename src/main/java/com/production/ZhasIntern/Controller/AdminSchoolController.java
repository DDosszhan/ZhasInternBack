package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.security.AccessPolicyService;
import com.production.ZhasIntern.service.SchoolImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/schools")
@RequiredArgsConstructor
public class AdminSchoolController {

    private final AccessPolicyService accessPolicyService;
    private final SchoolImportService schoolImportService;

    @PostMapping("/import")
    public SchoolImportService.SchoolImportResult importSchools(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "from", defaultValue = "0") int from
    ) {
        accessPolicyService.requireAdmin(jwt);
        return schoolImportService.importFromEgovV3(from);
    }
}
