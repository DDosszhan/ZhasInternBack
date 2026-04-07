package com.production.ZhasIntern.security;

import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.entity.UserRole;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessPolicyServiceTest {

    private ProfileRepository profileRepository;
    private AccessPolicyService service;

    @BeforeEach
    void setUp() {
        profileRepository = mock(ProfileRepository.class);
        service = new AccessPolicyService(profileRepository);
    }

    @Test
    void parseAllowedSelfSwitchRoleAcceptsSchoolCounselor() {
        UserRole role = service.parseAllowedSelfSwitchRole("school_counselor");

        assertEquals(UserRole.SCHOOL_COUNSELOR, role);
    }

    @Test
    void parseAllowedSelfSwitchRoleRejectsCounselorApprover() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.parseAllowedSelfSwitchRole("COUNSELOR_APPROVER"));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void requireApprovedSchoolCounselorReturnsVerifiedCounselor() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        profile.setRole(UserRole.SCHOOL_COUNSELOR);
        profile.setSchoolCounselorVerified(true);

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        UserProfile result = service.requireApprovedSchoolCounselor(jwt(userId));

        assertEquals(profile, result);
    }

    @Test
    void requireApprovedSchoolCounselorRejectsUnverifiedCounselor() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setId(userId);
        profile.setRole(UserRole.SCHOOL_COUNSELOR);
        profile.setSchoolCounselorVerified(false);

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requireApprovedSchoolCounselor(jwt(userId)));

        assertEquals("COUNSELOR_NOT_VERIFIED", ex.getCode());
    }

    private Jwt jwt(UUID userId) {
        return new Jwt(
                "token",
                null,
                null,
                Map.of("alg", "none"),
                Map.of("sub", userId.toString())
        );
    }
}
