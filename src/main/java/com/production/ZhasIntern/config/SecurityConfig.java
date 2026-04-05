package com.production.ZhasIntern.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SupabaseJwtConverter supabaseJwtConverter;

    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = "https://gexxcdtarifagzqjmvbu.supabase.co/auth/v1/.well-known/jwks.json";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(30000);

        RestTemplate restTemplate = new RestTemplate(factory);

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .restOperations(restTemplate)
                .build();

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        new JwtTimestampValidator(Duration.ofMinutes(5))
                );

        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/internships/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/internships/public").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/debug-token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/debug/auth").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/internships/mine").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/employer/internships").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employer/internships").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/employer/internships/*/applications").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/employer/internships/*/applications/*/status").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/profile/role").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/profile/student-details").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/school-counselor/verification-request").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/school-counselor/verification-request").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/school-counselor/students").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/school-counselor/students/*/applications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/counselor-approvals/requests").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/counselor-approvals/requests/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/applications/*/messages").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/applications/*/messages").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/students/*").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(supabaseJwtConverter)
                        )
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowCredentials(true);
        c.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://localhost:5173",
                "https://zhas-intern-front-end.vercel.app",
                "https://*.vercel.app"
        ));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
