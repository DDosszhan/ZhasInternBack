package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.Application;
import com.production.ZhasIntern.entity.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    boolean existsByInternshipIdAndStudentId(UUID internshipId, String studentId);

    Page<Application> findByInternshipIdOrderByCreatedAtDesc(UUID internshipId, Pageable pageable);

    Page<Application> findByInternshipIdAndStatusOrderByCreatedAtDesc(UUID internshipId, ApplicationStatus  status, Pageable pageable);

    Optional<Application> findByIdAndInternshipId(UUID id, UUID internshipId);

    // ✅ For "My applications" (student)
    Page<Application> findByStudentIdOrderByCreatedAtDesc(String studentId, Pageable pageable);

    Page<Application> findByStudentIdAndStatusOrderByCreatedAtDesc(String studentId, ApplicationStatus status, Pageable pageable);
}
