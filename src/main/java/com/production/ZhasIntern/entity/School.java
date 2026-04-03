package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schools", uniqueConstraints = {
        @UniqueConstraint(name = "uq_schools_source_external", columnNames = {"source", "source_version", "external_id"})
}, indexes = {
        @Index(name = "idx_schools_region_ru", columnList = "region_ru"),
        @Index(name = "idx_schools_region_kz", columnList = "region_kz"),
        @Index(name = "idx_schools_normalized_name", columnList = "normalized_name"),
        @Index(name = "idx_schools_school_name_ru", columnList = "school_name_ru"),
        @Index(name = "idx_schools_school_name_kz", columnList = "school_name_kz")
})
@Data
public class School {
    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_version", nullable = false, length = 20)
    private String sourceVersion;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "region_ru", length = 255)
    private String regionRu;

    @Column(name = "region_kz", length = 255)
    private String regionKz;

    @Column(name = "district_ru", length = 255)
    private String districtRu;

    @Column(name = "district_kz", length = 255)
    private String districtKz;

    @Column(name = "locality_ru", length = 255)
    private String localityRu;

    @Column(name = "locality_kz", length = 255)
    private String localityKz;

    @Column(name = "school_name_ru", length = 500)
    private String schoolNameRu;

    @Column(name = "school_name_kz", length = 500)
    private String schoolNameKz;

    @Column(name = "school_type_ru", length = 255)
    private String schoolTypeRu;

    @Column(name = "school_type_kz", length = 255)
    private String schoolTypeKz;

    @Column(name = "normalized_name", length = 500)
    private String normalizedName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "raw_json", columnDefinition = "text")
    private String rawJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
