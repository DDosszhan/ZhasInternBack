package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.SupportDtos;
import com.production.ZhasIntern.service.SupportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportDtos.ContactResponse createContactRequest(
            @RequestBody @Valid SupportDtos.ContactRequest request,
            HttpServletRequest httpRequest
    ) {
        return supportService.createContactRequest(request, httpRequest);
    }
}
