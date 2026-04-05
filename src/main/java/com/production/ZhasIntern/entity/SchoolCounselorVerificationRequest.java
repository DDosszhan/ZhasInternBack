package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "school_counselor_verification_requests")
@Data
public class SchoolCounselorVerificationRequest {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "counselor_id", nullable = false)
    private UUID counselorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School schoolEntity;

    @Column(name = "school_name", nullable = false, length = 500)
    private String schoolName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 100)
    private String contactPhone;

    @Column(name = "position_title", length = 255)
    private String positionTitle;

    @Column(name = "contacts_note", columnDefinition = "text")
    private String contactsNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchoolCounselorRequestStatus status = SchoolCounselorRequestStatus.PENDING;

    @Column(name = "review_comment", columnDefinition = "text")
    private String reviewComment;

    @Column(name = "reviewed_by", length = 128)
    private String reviewedBy;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
        if (status == null) {
            status = SchoolCounselorRequestStatus.PENDING;
        }
    }
}
