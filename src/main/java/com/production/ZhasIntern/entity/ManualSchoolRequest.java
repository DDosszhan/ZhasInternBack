package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "manual_school_requests")
@Data
public class ManualSchoolRequest {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 255)
    private String region;

    @Column(length = 255)
    private String district;

    @Column(length = 255)
    private String locality;

    @Column(name = "school_name", nullable = false, columnDefinition = "text")
    private String schoolName;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
