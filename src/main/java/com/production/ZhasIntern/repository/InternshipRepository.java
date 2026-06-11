package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.Application;
import com.production.ZhasIntern.entity.Internship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface InternshipRepository extends JpaRepository<Internship, UUID> {
    Page<Internship> findByStatusOrderByPublishedAtDesc(Internship.Status status, Pageable pageable);
    List<Internship> findByStatusOrderByPublishedAtDesc(Internship.Status status);
    Optional<Internship> findByIdAndStatus(UUID id, Internship.Status status);

    Page<Internship> findByEmployerIdOrderByCreatedAtDesc(String employerId, Pageable pageable);
    Optional<Internship> findByIdAndEmployerId(UUID id, String employerId);

    // ✅ Needed to resolve internship titles in bulk for "My applications"
    Set<Internship> findByIdIn(Iterable<UUID> ids);
}
