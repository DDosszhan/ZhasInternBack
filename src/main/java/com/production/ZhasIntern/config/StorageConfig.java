package com.production.ZhasIntern.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@Slf4j
public class StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        AwsBasicCredentials credentials = createCredentials(properties);
        logStorageConfiguration(properties);

        return S3Client.builder()
                .endpointOverride(URI.create(requireConfigured(properties.getS3Endpoint(), "app.storage.s3-endpoint")))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(requireConfigured(properties.getRegion(), "app.storage.region")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties properties) {
        AwsBasicCredentials credentials = createCredentials(properties);

        return S3Presigner.builder()
                .endpointOverride(URI.create(requireConfigured(properties.getS3Endpoint(), "app.storage.s3-endpoint")))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(requireConfigured(properties.getRegion(), "app.storage.region")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private AwsBasicCredentials createCredentials(StorageProperties properties) {
        String accessKey = requireConfigured(properties.getAccessKey(), "app.storage.access-key");
        String secretKey = requireConfigured(properties.getSecretKey(), "app.storage.secret-key");
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    private String requireConfigured(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required storage configuration: " + propertyName);
        }
        return value.trim();
    }

    private void logStorageConfiguration(StorageProperties properties) {
        log.info(
                "Supabase storage config loaded: endpoint={}, region={}, profileBucket={}, applicationBucket={}, accessKeyPrefix={}",
                properties.getS3Endpoint(),
                properties.getRegion(),
                properties.getProfilePhotosBucket(),
                properties.getApplicationFilesBucket(),
                redactAccessKey(properties.getAccessKey())
        );
    }

    private String redactAccessKey(String accessKey) {
        if (!StringUtils.hasText(accessKey)) {
            return "missing";
        }
        String trimmed = accessKey.trim();
        return trimmed.length() <= 8 ? trimmed : trimmed.substring(0, 8) + "...";
    }
}
