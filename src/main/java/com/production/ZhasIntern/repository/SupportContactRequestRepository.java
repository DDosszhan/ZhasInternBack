package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.SupportContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface SupportContactRequestRepository extends JpaRepository<SupportContactRequest, UUID> {
    long countByEmailIgnoreCaseAndCreatedAtAfter(String email, Instant createdAtAfter);
    long countByIpAddressAndCreatedAtAfter(String ipAddress, Instant createdAtAfter);
}
