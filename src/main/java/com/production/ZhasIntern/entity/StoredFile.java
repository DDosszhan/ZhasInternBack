package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "stored_files",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bucket_name", "object_path"})
)
@Data
public class StoredFile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, length = 128)
    private String ownerUserId;

    @Column(name = "application_id")
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StoredFilePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StoredFileStatus status = StoredFileStatus.PENDING;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @Column(name = "object_path", nullable = false, length = 1000)
    private String objectPath;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 200)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = StoredFileStatus.PENDING;
        }
    }
}
