package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.KatoDtos;
import com.production.ZhasIntern.service.KatoDirectoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/kato")
@RequiredArgsConstructor
public class KatoController {

    private final KatoDirectoryService katoDirectoryService;

    @GetMapping("/regions")
    public KatoDtos.KatoListResponse listRegions() {
        return katoDirectoryService.listRegions();
    }

    @GetMapping("/children")
    public KatoDtos.KatoListResponse listChildren(@RequestParam Integer parentId) {
        return katoDirectoryService.listChildren(parentId);
    }

    @GetMapping("/localities")
    public KatoDtos.KatoListResponse listLocalities(
            @RequestParam(required = false) Integer regionId,
            @RequestParam(required = false) Integer districtId
    ) {
        return katoDirectoryService.listLocalities(regionId, districtId);
    }
}
