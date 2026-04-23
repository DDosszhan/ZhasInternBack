package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "message_attachments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "file_id"})
)
@Data
public class MessageAttachment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
