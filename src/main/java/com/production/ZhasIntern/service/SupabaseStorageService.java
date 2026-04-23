package com.production.ZhasIntern.service;

import com.production.ZhasIntern.config.StorageProperties;
import com.production.ZhasIntern.dto.FileDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final StorageProperties storageProperties;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public FileDtos.UploadTargetResponse createUploadTarget(String bucketName, String objectPath, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectPath)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .putObjectRequest(objectRequest)
                        .signatureDuration(storageProperties.getUploadUrlDuration())
                        .build()
        );

        log.info(
                "Generated presigned upload URL: method={}, bucket={}, key={}, contentType={}, expiresInSeconds={}, region={}, signedHeaders={}, browserExecutable={}",
                presignedRequest.httpRequest().method(),
                bucketName,
                objectPath,
                contentType,
                storageProperties.getUploadUrlDuration().getSeconds(),
                storageProperties.getRegion(),
                presignedRequest.signedHeaders().keySet(),
                presignedRequest.isBrowserExecutable()
        );

        return new FileDtos.UploadTargetResponse(
                null,
                presignedRequest.url().toString(),
                "PUT",
                Map.of("Content-Type", contentType),
                Instant.now().plus(storageProperties.getUploadUrlDuration())
        );
    }

    public FileDtos.DownloadUrlResponse createDownloadTarget(java.util.UUID fileId, String bucketName, String objectPath) {
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(objectPath)
                                .build())
                        .signatureDuration(storageProperties.getDownloadUrlDuration())
                        .build()
        );

        log.info(
                "Generated presigned download URL: method={}, bucket={}, key={}, expiresInSeconds={}, region={}, signedHeaders={}",
                presignedRequest.httpRequest().method(),
                bucketName,
                objectPath,
                storageProperties.getDownloadUrlDuration().getSeconds(),
                storageProperties.getRegion(),
                presignedRequest.signedHeaders().keySet()
        );

        return new FileDtos.DownloadUrlResponse(
                fileId,
                presignedRequest.url().toString(),
                Instant.now().plus(storageProperties.getDownloadUrlDuration())
        );
    }

    public boolean objectExists(String bucketName, String objectPath) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectPath)
                    .build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public String buildPublicObjectUrl(String bucketName, String objectPath) {
        URI endpoint = URI.create(storageProperties.getS3Endpoint());
        String base = endpoint.toString().replace("/storage/v1/s3", "");
        return UriComponentsBuilder.fromUriString(base)
                .path("/storage/v1/object/public/")
                .pathSegment(bucketName)
                .path("/")
                .path(objectPath)
                .build()
                .toUriString();
    }
}
