package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.ManualSchoolRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ManualSchoolRequestRepository extends JpaRepository<ManualSchoolRequest, UUID> {
}
