package com.production.ZhasIntern.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "test_connection_table")
@Data
public class TestUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String something;
}
