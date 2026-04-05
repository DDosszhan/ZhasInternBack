package com.production.ZhasIntern.entity;

import com.production.ZhasIntern.config.ApplicationAnswersJsonConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"internship_id", "student_id"})
)
@Data
public class Application {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "internship_id", nullable = false)
    private UUID internshipId;

    @Column(name = "student_id", nullable = false, length = 100)
    private String studentId; // Supabase sub (uuid string)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.SUBMITTED;

    // ✅ Proper jsonb mapping (Hibernate will bind it as JSON, not varchar)
    @Convert(converter = ApplicationAnswersJsonConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "answers", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> answers = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) status = ApplicationStatus.SUBMITTED;
        if (answers == null) answers = new HashMap<>();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (answers == null) answers = new HashMap<>();
    }
}
