package com.production.ZhasIntern.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthDebugController {

    private final JwtDecoder jwtDecoder;

    @GetMapping("/api/public/debug-token")
    public ResponseEntity<?> debugToken(
            @RequestParam("token") String token
    ) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            return ResponseEntity.ok(Map.of(
                    "subject", jwt.getSubject(),
                    "issuedAt", String.valueOf(jwt.getIssuedAt()),
                    "expiresAt", String.valueOf(jwt.getExpiresAt()),
                    "claims", jwt.getClaims()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", e.getClass().getName(),
                            "message", e.getMessage()
                    ));
        }
    }
}