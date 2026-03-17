package com.production.ZhasIntern.service;

import com.production.ZhasIntern.entity.ApplicationStatus;
import com.production.ZhasIntern.entity.Internship;
import com.production.ZhasIntern.repository.ApplicationRepository;
import com.production.ZhasIntern.repository.InternshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployerApplicationService {

    private final ApplicationRepository applicationRepository;
    private final InternshipRepository internshipRepository;

    @Transactional
    public void updateStatus(UUID internshipId, UUID applicationId, ApplicationStatus newStatus, String employerUserId) {

        // Ensure internship belongs to this employer
        Internship it = internshipRepository.findByIdAndEmployerId(internshipId, employerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Internship not found"));

        var app = applicationRepository.findByIdAndInternshipId(applicationId, it.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        app.setStatus(newStatus);
        applicationRepository.save(app);
    }
}