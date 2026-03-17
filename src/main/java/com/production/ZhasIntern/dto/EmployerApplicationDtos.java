package com.production.ZhasIntern.dto;

import com.production.ZhasIntern.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public class EmployerApplicationDtos {

    public record UpdateApplicationStatusRequest(
            @NotNull ApplicationStatus status
    ) {}
}