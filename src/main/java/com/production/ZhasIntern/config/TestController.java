package com.production.ZhasIntern.config;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    // Доступен всем
    @GetMapping("/public/hello")
    public String publicHello() {
        return "Это публичный эндпоинт, токен не нужен!";
    }

    // Доступен только с токеном
    @GetMapping("/private/me")
    public Map<String, String> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        // Spring сам распарсил токен и положил его в объект Jwt
        return Map.of(
                "message", "Привет! Ты авторизован.",
                "userId", jwt.getSubject(), // Это UUID пользователя из Supabase (auth.users)
                "email", jwt.getClaimAsString("email")
        );
    }
}
