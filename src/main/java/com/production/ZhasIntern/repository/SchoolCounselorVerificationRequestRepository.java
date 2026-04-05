package com.production.ZhasIntern.repository;

import com.production.ZhasIntern.entity.SchoolCounselorRequestStatus;
import com.production.ZhasIntern.entity.SchoolCounselorVerificationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchoolCounselorVerificationRequestRepository extends JpaRepository<SchoolCounselorVerificationRequest, UUID> {

    boolean existsByCounselorIdAndStatus(UUID counselorId, SchoolCounselorRequestStatus status);

    Optional<SchoolCounselorVerificationRequest> findTopByCounselorIdOrderBySubmittedAtDesc(UUID counselorId);

    Page<SchoolCounselorVerificationRequest> findByStatusOrderBySubmittedAtDesc(SchoolCounselorRequestStatus status, Pageable pageable);

    Page<SchoolCounselorVerificationRequest> findAllByOrderBySubmittedAtDesc(Pageable pageable);
}
