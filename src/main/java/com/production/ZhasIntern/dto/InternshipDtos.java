package com.production.ZhasIntern.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class InternshipDtos {

    public record PublicItem(
            UUID id,
            String title,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Instant deadline,
            String companyName,
            String location,
            String type,
            Boolean isRemote,
            String shortDescription
    ) {}

    public record PublicDetails(
            UUID id,
            String title,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Instant deadline,
            String companyName,
            String location,
            String type,
            Boolean isRemote,
            String shortDescription,
            String description
    ) {}

    public record CreateRequest(
            @NotBlank(message = "title is required")
            @Size(max = 200, message = "title max length is 200")
            String title,

            @NotBlank(message = "companyName is required")
            @Size(max = 200, message = "companyName max length is 200")
            String companyName,

            @Size(max = 200, message = "location max length is 200")
            String location,

            @NotBlank(message = "type is required")
            String type,

            @NotNull(message = "isRemote is required")
            Boolean isRemote,

            @Size(max = 2000, message = "shortDescription max length is 2000")
            String shortDescription,

            @Size(max = 20000, message = "description max length is 20000")
            String description,

            Instant deadline
    ) {}

    public record CreateResponse(UUID id) {}

    public record MineItem(
            UUID id,
            String title,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            Instant deadline,
            String companyName,
            String location,
            String type,
            Boolean isRemote,
            String shortDescription,
            String description
    ) {}
}