package com.production.ZhasIntern.service;

import com.production.ZhasIntern.config.StorageProperties;
import com.production.ZhasIntern.dto.FileDtos;
import com.production.ZhasIntern.entity.StoredFile;
import com.production.ZhasIntern.entity.StoredFilePurpose;
import com.production.ZhasIntern.entity.StoredFileStatus;
import com.production.ZhasIntern.entity.UserProfile;
import com.production.ZhasIntern.exception.ApiException;
import com.production.ZhasIntern.repository.ProfileRepository;
import com.production.ZhasIntern.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StoredFileService {

    private static final Set<String> PROFILE_PHOTO_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> APPLICATION_ATTACHMENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Pattern SAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

    private final StoredFileRepository storedFileRepository;
    private final ProfileRepository profileRepository;
    private final StorageProperties storageProperties;
    private final SupabaseStorageService supabaseStorageService;
    private final ApplicationAccessService applicationAccessService;

    @Transactional
    public FileDtos.UploadTargetResponse createProfilePhotoUploadTarget(String currentUserId, FileDtos.InitiateUploadRequest request) {
        validateUpload(request, PROFILE_PHOTO_TYPES, storageProperties.getProfilePhotoMaxBytes(), "Unsupported profile photo file type");

        StoredFile storedFile = new StoredFile();
        storedFile.setOwnerUserId(currentUserId);
        storedFile.setPurpose(StoredFilePurpose.PROFILE_PHOTO);
        storedFile.setStatus(StoredFileStatus.PENDING);
        storedFile.setBucketName(storageProperties.getProfilePhotosBucket());
        storedFile.setOriginalFileName(cleanFileName(request.fileName()));
        storedFile.setContentType(normalizeContentType(request.contentType()));
        storedFile.setSizeBytes(request.sizeBytes());
        storedFile.setObjectPath(buildObjectPath("profiles/" + currentUserId + "/photo", storedFile.getOriginalFileName()));
        StoredFile saved = storedFileRepository.save(storedFile);

        FileDtos.UploadTargetResponse uploadTarget = supabaseStorageService.createUploadTarget(
                saved.getBucketName(),
                saved.getObjectPath(),
                saved.getContentType()
        );

        return new FileDtos.UploadTargetResponse(saved.getId(), uploadTarget.uploadUrl(), uploadTarget.method(), uploadTarget.headers(), uploadTarget.expiresAt());
    }

    @Transactional
    public FileDtos.CompleteUploadResponse completeProfilePhotoUpload(UUID fileId, String currentUserId) {
        StoredFile storedFile = loadOwnedFile(fileId, currentUserId, StoredFilePurpose.PROFILE_PHOTO);
        ensureObjectUploaded(storedFile);

        UserProfile profile = profileRepository.findById(UUID.fromString(currentUserId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Profile not found"));
        profile.setProfilePhotoFileId(storedFile.getId());
        profileRepository.save(profile);

        return new FileDtos.CompleteUploadResponse(storedFile.getId(), storedFile.getStatus().name(), resolvePublicUrl(storedFile));
    }

    @Transactional
    public FileDtos.UploadTargetResponse createApplicationAttachmentUploadTarget(UUID applicationId, String currentUserId, FileDtos.InitiateUploadRequest request) {
        applicationAccessService.requireChatWriteAccess(applicationId, currentUserId);
        validateUpload(request, APPLICATION_ATTACHMENT_TYPES, storageProperties.getApplicationFileMaxBytes(), "Unsupported application file type");

        StoredFile storedFile = new StoredFile();
        storedFile.setOwnerUserId(currentUserId);
        storedFile.setApplicationId(applicationId);
        storedFile.setPurpose(StoredFilePurpose.CHAT_ATTACHMENT);
        storedFile.setStatus(StoredFileStatus.PENDING);
        storedFile.setBucketName(storageProperties.getApplicationFilesBucket());
        storedFile.setOriginalFileName(cleanFileName(request.fileName()));
        storedFile.setContentType(normalizeContentType(request.contentType()));
        storedFile.setSizeBytes(request.sizeBytes());
        storedFile.setObjectPath(buildObjectPath("applications/" + applicationId + "/" + currentUserId, storedFile.getOriginalFileName()));
        StoredFile saved = storedFileRepository.save(storedFile);

        FileDtos.UploadTargetResponse uploadTarget = supabaseStorageService.createUploadTarget(
                saved.getBucketName(),
                saved.getObjectPath(),
                saved.getContentType()
        );

        return new FileDtos.UploadTargetResponse(saved.getId(), uploadTarget.uploadUrl(), uploadTarget.method(), uploadTarget.headers(), uploadTarget.expiresAt());
    }

    @Transactional
    public FileDtos.CompleteUploadResponse completeApplicationAttachmentUpload(UUID applicationId, UUID fileId, String currentUserId) {
        applicationAccessService.requireChatWriteAccess(applicationId, currentUserId);
        StoredFile storedFile = loadOwnedFile(fileId, currentUserId, StoredFilePurpose.CHAT_ATTACHMENT);
        if (!applicationId.equals(storedFile.getApplicationId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "File does not belong to this application");
        }
        ensureObjectUploaded(storedFile);

        FileDtos.DownloadUrlResponse download = supabaseStorageService.createDownloadTarget(
                storedFile.getId(),
                storedFile.getBucketName(),
                storedFile.getObjectPath()
        );

        return new FileDtos.CompleteUploadResponse(storedFile.getId(), storedFile.getStatus().name(), download.downloadUrl());
    }

    public FileDtos.DownloadUrlResponse createDownloadUrl(UUID fileId, String currentUserId) {
        StoredFile storedFile = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "File not found"));

        authorizeFileRead(storedFile, currentUserId);

        if (storedFile.getPurpose() == StoredFilePurpose.PROFILE_PHOTO) {
            if (storedFile.getStatus() != StoredFileStatus.READY) {
                throw new ApiException(HttpStatus.CONFLICT, "UPLOAD_NOT_READY", "Profile photo upload is not completed yet");
            }
            return new FileDtos.DownloadUrlResponse(storedFile.getId(), resolvePublicUrl(storedFile), null);
        }
        if (storedFile.getStatus() != StoredFileStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "UPLOAD_NOT_READY", "File upload is not completed yet");
        }

        return supabaseStorageService.createDownloadTarget(
                storedFile.getId(),
                storedFile.getBucketName(),
                storedFile.getObjectPath()
        );
    }

    public String resolveProfilePhotoUrl(UUID fileId) {
        if (fileId == null) {
            return null;
        }

        StoredFile storedFile = storedFileRepository.findById(fileId).orElse(null);
        if (storedFile == null || storedFile.getStatus() != StoredFileStatus.READY || storedFile.getPurpose() != StoredFilePurpose.PROFILE_PHOTO) {
            return null;
        }
        if (!storageProperties.isProfilePhotosPublic()) {
            return null;
        }
        return resolvePublicUrl(storedFile);
    }

    public String resolveCurrentUserProfilePhotoUrl(UUID fileId, String currentUserId) {
        if (fileId == null) {
            return null;
        }

        StoredFile storedFile = storedFileRepository.findById(fileId).orElse(null);
        if (storedFile == null || storedFile.getStatus() != StoredFileStatus.READY || storedFile.getPurpose() != StoredFilePurpose.PROFILE_PHOTO) {
            return null;
        }

        if (!currentUserId.equals(storedFile.getOwnerUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this profile photo");
        }

        if (storageProperties.isProfilePhotosPublic()) {
            return resolvePublicUrl(storedFile);
        }

        return supabaseStorageService.createDownloadTarget(
                storedFile.getId(),
                storedFile.getBucketName(),
                storedFile.getObjectPath()
        ).downloadUrl();
    }

    public Map<UUID, StoredFile> loadFilesByIds(List<UUID> fileIds) {
        return storedFileRepository.findByIdIn(fileIds).stream().collect(java.util.stream.Collectors.toMap(StoredFile::getId, file -> file));
    }

    public FileDtos.AttachmentItem toAttachmentItem(StoredFile storedFile) {
        FileDtos.DownloadUrlResponse download = supabaseStorageService.createDownloadTarget(
                storedFile.getId(),
                storedFile.getBucketName(),
                storedFile.getObjectPath()
        );

        return new FileDtos.AttachmentItem(
                storedFile.getId(),
                storedFile.getOriginalFileName(),
                storedFile.getContentType(),
                storedFile.getSizeBytes(),
                download.downloadUrl(),
                download.expiresAt()
        );
    }

    private void authorizeFileRead(StoredFile storedFile, String currentUserId) {
        if (storedFile.getPurpose() == StoredFilePurpose.PROFILE_PHOTO) {
            return;
        }

        if (storedFile.getPurpose() == StoredFilePurpose.CHAT_ATTACHMENT) {
            if (storedFile.getApplicationId() == null) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Application file is misconfigured");
            }
            applicationAccessService.ensureCanReadChat(storedFile.getApplicationId(), currentUserId);
            return;
        }

        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this file");
    }

    private void validateUpload(FileDtos.InitiateUploadRequest request, Set<String> allowedContentTypes, long maxBytes, String invalidTypeMessage) {
        String normalizedContentType = normalizeContentType(request.contentType());
        if (!allowedContentTypes.contains(normalizedContentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", invalidTypeMessage);
        }
        if (request.sizeBytes() == null || request.sizeBytes() <= 0 || request.sizeBytes() > maxBytes) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "File size exceeds the configured limit",
                    Map.of("maxBytes", maxBytes)
            );
        }
    }

    private StoredFile loadOwnedFile(UUID fileId, String currentUserId, StoredFilePurpose purpose) {
        StoredFile storedFile = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "File not found"));

        if (!currentUserId.equals(storedFile.getOwnerUserId()) || storedFile.getPurpose() != purpose) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have access to this file");
        }

        return storedFile;
    }

    private void ensureObjectUploaded(StoredFile storedFile) {
        if (!supabaseStorageService.objectExists(storedFile.getBucketName(), storedFile.getObjectPath())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_NOT_FOUND", "Upload was not found in storage");
        }
        if (storedFile.getStatus() != StoredFileStatus.READY) {
            storedFile.setStatus(StoredFileStatus.READY);
            storedFile.setUploadedAt(Instant.now());
            storedFileRepository.save(storedFile);
        }
    }

    private String resolvePublicUrl(StoredFile storedFile) {
        return supabaseStorageService.buildPublicObjectUrl(storedFile.getBucketName(), storedFile.getObjectPath());
    }

    private String buildObjectPath(String prefix, String fileName) {
        return prefix + "/" + UUID.randomUUID() + "-" + cleanFileName(fileName);
    }

    private String cleanFileName(String fileName) {
        String normalized = fileName == null ? "file" : fileName.trim();
        if (normalized.isEmpty()) {
            normalized = "file";
        }
        return SAFE_CHARS.matcher(normalized).replaceAll("_");
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }
}
