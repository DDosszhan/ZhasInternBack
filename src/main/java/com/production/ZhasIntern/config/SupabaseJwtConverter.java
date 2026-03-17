package com.production.ZhasIntern.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class SupabaseJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<SimpleGrantedAuthority> auth = new HashSet<>();

        String role = extractRole(jwt);
        if (role != null && !role.isBlank()) {
            auth.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
        }

        return new JwtAuthenticationToken(jwt, auth, jwt.getSubject());
    }

    private String extractRole(Jwt jwt) {
        Object appMeta = jwt.getClaims().get("app_metadata");
        if (appMeta instanceof Map<?, ?> m && m.get("role") instanceof String s) {
            return s;
        }

        Object userMeta = jwt.getClaims().get("user_metadata");
        if (userMeta instanceof Map<?, ?> m && m.get("role") instanceof String s) {
            return s;
        }

        Object direct = jwt.getClaims().get("role");
        if (direct instanceof String s) {
            return s;
        }

        return null;
    }
}