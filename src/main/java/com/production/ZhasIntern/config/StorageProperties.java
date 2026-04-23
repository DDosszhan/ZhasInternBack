package com.production.ZhasIntern.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String s3Endpoint;
    private String region = "ap-northeast-2";
    private String accessKey;
    private String secretKey;
    private String profilePhotosBucket = "profile-photos";
    private String applicationFilesBucket = "application-files";
    private boolean profilePhotosPublic = true;
    private Duration uploadUrlDuration = Duration.ofMinutes(15);
    private Duration downloadUrlDuration = Duration.ofMinutes(15);
    private long profilePhotoMaxBytes = 10L * 1024 * 1024;
    private long applicationFileMaxBytes = 10L * 1024 * 1024;

    public String getS3Endpoint() {
        return s3Endpoint;
    }

    public void setS3Endpoint(String s3Endpoint) {
        this.s3Endpoint = s3Endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getProfilePhotosBucket() {
        return profilePhotosBucket;
    }

    public void setProfilePhotosBucket(String profilePhotosBucket) {
        this.profilePhotosBucket = profilePhotosBucket;
    }

    public String getApplicationFilesBucket() {
        return applicationFilesBucket;
    }

    public void setApplicationFilesBucket(String applicationFilesBucket) {
        this.applicationFilesBucket = applicationFilesBucket;
    }

    public boolean isProfilePhotosPublic() {
        return profilePhotosPublic;
    }

    public void setProfilePhotosPublic(boolean profilePhotosPublic) {
        this.profilePhotosPublic = profilePhotosPublic;
    }

    public Duration getUploadUrlDuration() {
        return uploadUrlDuration;
    }

    public void setUploadUrlDuration(Duration uploadUrlDuration) {
        this.uploadUrlDuration = uploadUrlDuration;
    }

    public Duration getDownloadUrlDuration() {
        return downloadUrlDuration;
    }

    public void setDownloadUrlDuration(Duration downloadUrlDuration) {
        this.downloadUrlDuration = downloadUrlDuration;
    }

    public long getProfilePhotoMaxBytes() {
        return profilePhotoMaxBytes;
    }

    public void setProfilePhotoMaxBytes(long profilePhotoMaxBytes) {
        this.profilePhotoMaxBytes = profilePhotoMaxBytes;
    }

    public long getApplicationFileMaxBytes() {
        return applicationFileMaxBytes;
    }

    public void setApplicationFileMaxBytes(long applicationFileMaxBytes) {
        this.applicationFileMaxBytes = applicationFileMaxBytes;
    }
}
