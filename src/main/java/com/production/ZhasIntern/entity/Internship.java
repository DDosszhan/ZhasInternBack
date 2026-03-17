package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "internships")
@Data
public class Internship {

    public enum Status {DRAFT, PUBLISHED, ARCHIVED}

    public enum WorkType {ONSITE, REMOTE, HYBRID}

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkType type = WorkType.ONSITE;

    @Column(nullable = false)
    private Boolean isRemote = false;

    @Column(length = 2000)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    private Instant deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    private Instant publishedAt;

    @Column(name = "employer_id", nullable = false, length = 128)
    private String employerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = Status.DRAFT;
        if (type == null) type = WorkType.ONSITE;
        if (isRemote == null) isRemote = false;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}