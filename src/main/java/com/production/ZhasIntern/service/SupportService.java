package com.production.ZhasIntern.service;

import com.production.ZhasIntern.dto.SupportDtos;
import com.production.ZhasIntern.entity.SupportContactRequest;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.SupportContactRequestRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupportService {

    private static final long MAX_PER_HOUR_PER_EMAIL = 3;
    private static final long MAX_PER_HOUR_PER_IP = 10;

    private final SupportContactRequestRepository supportContactRequestRepository;

    @Transactional
    public SupportDtos.ContactResponse createContactRequest(SupportDtos.ContactRequest request, HttpServletRequest httpRequest) {
        String email = clean(request.email());
        String ipAddress = resolveClientIp(httpRequest);
        Instant since = Instant.now().minus(1, ChronoUnit.HOURS);

        long emailRequests = supportContactRequestRepository.countByEmailIgnoreCaseAndCreatedAtAfter(email, since);
        if (emailRequests >= MAX_PER_HOUR_PER_EMAIL) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMITED",
                    "Too many support requests from this email. Please try again later.",
                    Map.of("window", "1h", "limit", MAX_PER_HOUR_PER_EMAIL)
            );
        }

        if (ipAddress != null) {
            long ipRequests = supportContactRequestRepository.countByIpAddressAndCreatedAtAfter(ipAddress, since);
            if (ipRequests >= MAX_PER_HOUR_PER_IP) {
                throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "RATE_LIMITED",
                        "Too many support requests from this IP. Please try again later.",
                        Map.of("window", "1h", "limit", MAX_PER_HOUR_PER_IP)
                );
            }
        }

        SupportContactRequest entity = new SupportContactRequest();
        entity.setName(clean(request.name()));
        entity.setEmail(email);
        entity.setCategory(clean(request.category()).toLowerCase(Locale.ROOT));
        entity.setMessage(clean(request.message()));
        entity.setIpAddress(ipAddress);

        supportContactRequestRepository.save(entity);
        return new SupportDtos.ContactResponse(true, "Support request submitted successfully");
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
