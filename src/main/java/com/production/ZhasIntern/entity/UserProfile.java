package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profiles")
@Data
public class UserProfile {
    @Id
    private UUID id; // Будет совпадать с UUID из Supabase Auth

    @Column(unique = true)
    private String email;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private UserRole role; // STUDENT или EMPLOYER

    private String bio; // О себе или о компании

    private String school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School schoolEntity;

    @Column(name = "manual_school_name", length = 500)
    private String manualSchoolName;

    private String grade;

    private String city;

    private String portfolio;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
