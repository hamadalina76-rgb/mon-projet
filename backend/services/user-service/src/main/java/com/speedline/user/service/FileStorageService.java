package com.speedline.user.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final String STORAGE_TYPE_GCS = "gcs";

    @Value("${file.storage.type:local}")
    private String storageType;

    @Value("${file.upload.dir:uploads/profile-pictures}")
    private String uploadDir;

    @Value("${file.upload.base-url:http://localhost:8082/uploads}")
    private String baseUrl;

    @Value("${file.gcs.bucket:}")
    private String gcsBucket;

    @Value("${spring.cloud.gcp.project-id:}")
    private String gcpProjectId;

    private Storage storage;

    @PostConstruct
    public void init() {
        if (isGcsStorage()) {
            this.storage = gcpProjectId == null || gcpProjectId.isBlank()
                    ? StorageOptions.getDefaultInstance().getService()
                    : StorageOptions.newBuilder().setProjectId(gcpProjectId).build().getService();
            log.info("User file storage initialized in GCS mode for bucket={}", gcsBucket);
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            log.info("User file storage initialized locally at {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not create user upload directory", e);
        }
    }

    public String storeProfilePicture(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "profile.png" : file.getOriginalFilename());
        String filename = UUID.randomUUID() + extensionOf(originalFilename);
        return store(file, "", filename);
    }

    public String storeCourierDocument(MultipartFile file, String directory, String filenamePrefix) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document.bin" : file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        String normalizedDirectory = directory.replace("\\", "/");
        String trimmed = normalizedDirectory.endsWith("/") ? normalizedDirectory.substring(0, normalizedDirectory.length() - 1) : normalizedDirectory;
        String[] parts = trimmed.split("/");
        String userIdPart = parts.length == 0 ? "unknown" : parts[parts.length - 1];
        String filename = filenamePrefix + userIdPart + extension;
        return store(file, normalizedDirectory, filename);
    }

    private String store(MultipartFile file, String directory, String filename) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Cannot store empty file");
        }
        return isGcsStorage()
                ? storeInGcs(file, directory, filename)
                : storeLocally(file, directory, filename);
    }

    private String storeLocally(MultipartFile file, String directory, String filename) {
        try {
            Path basePath = Paths.get(uploadDir);
            Path targetDir = directory == null || directory.isBlank() ? basePath : basePath.resolve(directory);
            Files.createDirectories(targetDir);

            Path targetLocation = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String relative = directory == null || directory.isBlank() ? filename : directory + "/" + filename;
            relative = relative.replace("\\", "/");
            String fileUrl = baseUrl + "/" + relative;
            log.info("User file stored locally at {}", targetLocation.toAbsolutePath());
            return fileUrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file locally: " + e.getMessage(), e);
        }
    }

    private String storeInGcs(MultipartFile file, String directory, String filename) {
        try {
            if (gcsBucket == null || gcsBucket.isBlank()) {
                throw new IllegalStateException("file.gcs.bucket must be configured when file.storage.type=gcs");
            }

            String normalizedDirectory = directory == null ? "" : directory.replace("\\", "/");
            String objectName = normalizedDirectory == null || normalizedDirectory.isBlank()
                    ? filename
                    : normalizedDirectory + "/" + filename;

            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(gcsBucket, objectName))
                    .setContentType(resolveContentType(file, filename))
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                storage.createFrom(blobInfo, inputStream);
            }

            String publicUrl = "https://storage.googleapis.com/" + gcsBucket + "/" + objectName;
            log.info("User file stored in GCS bucket={} object={}", gcsBucket, objectName);
            return publicUrl;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to GCS: " + e.getMessage(), e);
        }
    }

    private String extensionOf(String originalFilename) {
        return originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
    }

    private String resolveContentType(MultipartFile file, String filename) {
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            return file.getContentType();
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    private boolean isGcsStorage() {
        return STORAGE_TYPE_GCS.equalsIgnoreCase(storageType);
    }
}
