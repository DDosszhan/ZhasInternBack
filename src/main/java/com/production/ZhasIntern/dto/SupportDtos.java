package com.production.ZhasIntern.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SupportDtos {

    public record ContactRequest(
            @NotBlank(message = "name is required")
            @Size(max = 150, message = "name max length is 150")
            String name,
            @NotBlank(message = "email is required")
            @Email(message = "email must be valid")
            @Size(max = 255, message = "email max length is 255")
            String email,
            @NotBlank(message = "category is required")
            @Size(max = 50, message = "category max length is 50")
            String category,
            @NotBlank(message = "message is required")
            @Size(max = 5000, message = "message max length is 5000")
            String message
    ) {}

    public record ContactResponse(
            boolean success,
            String message
    ) {}
}
