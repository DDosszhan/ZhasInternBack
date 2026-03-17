package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

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
}

