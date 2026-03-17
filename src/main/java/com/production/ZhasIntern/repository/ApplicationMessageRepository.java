package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.ApplicationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationMessageRepository extends JpaRepository<ApplicationMessage, UUID> {
    List<ApplicationMessage> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}