package com.production.ZhasIntern.Controller;

import com.production.ZhasIntern.dto.StudentProfileDtos;
import com.production.ZhasIntern.service.StudentProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping("/{id}")
    public StudentProfileDtos.StudentProfileResponse getStudentProfile(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return studentProfileService.getStudentProfile(id, jwt.getSubject());
    }
}
