package com.speedline.partner.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * Service de stockage des fichiers (images, documents)
 * Stocke les fichiers sur le système de fichiers local
 * Les fichiers sont accessibles via /uploads/...
 */
@Service
@Slf4j
public class FileStorageService {

    private static final String STORAGE_TYPE_LOCAL = "local";
    private static final String STORAGE_TYPE_GCS = "gcs";

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8083/uploads}")
    private String baseUrl;

    @Value("${file.storage.type:local}")
    private String storageType;

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
            log.info("File storage initialized in GCS mode for bucket={}", gcsBucket);
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Files.createDirectories(uploadPath.resolve("partners/logos"));
            Files.createDirectories(uploadPath.resolve("partners/covers"));
            Files.createDirectories(uploadPath.resolve("partners/photos"));
            Files.createDirectories(uploadPath.resolve("partners/documents"));
            Files.createDirectories(uploadPath.resolve("partners/products"));
            Files.createDirectories(uploadPath.resolve("partners/categories"));
            log.info("File upload directories created at: {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create upload directories", e);
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    /**
     * Stocker un fichier image (logo ou cover)
     *
     * @param file le fichier à stocker
     * @param partnerId l'ID du partenaire
     * @param type le type d'image (logos, covers, photos)
     * @return l'URL publique du fichier
     */
    public String storeImage(MultipartFile file, Long partnerId, String type) {
        return storeFile(file, "partners/" + type, partnerId);
    }

    /**
     * Stocker un document
     *
     * @param file le fichier à stocker
     * @param partnerId l'ID du partenaire
     * @param docType le type de document (kbis, idCard, insurance, rib)
     * @return l'URL publique du fichier
     */
    public String storeDocument(MultipartFile file, Long partnerId, String docType) {
        return storeFile(file, "partners/documents", partnerId);
    }
    /**
     * Stocker une image produit.
     */
    public String storeProductImage(MultipartFile file, Long partnerId, Long productId) {
        return storeFileWithSuffix(file, "partners/products", partnerId, "p" + productId);
    }

    /**
     * Stocker une image de catégorie menu.
     */
    public String storeCategoryImage(MultipartFile file, Long partnerId, Long catId) {
        return storeFileWithSuffix(file, "partners/categories", partnerId, "c" + catId);
    }

    /**
     * Stocker une icône de catégorie
     *
     * @param file le fichier image (PNG, JPG, SVG, WEBP)
     * @return l'URL publique du fichier
     */
    public String storeIcon(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Le fichier est vide");
            }

            // Dériver l'extension depuis le nom, puis depuis le Content-Type si nom absent
            String extension = resolveExtension(file);

            String filename = UUID.randomUUID().toString() + extension;
            Path targetDir = Paths.get(uploadDir, "categories", "icons");
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Category icon stored: {}", targetPath.toAbsolutePath());
            return baseUrl + "/categories/icons/" + filename;
        } catch (IllegalArgumentException e) {
            throw e; // Remonter telle quelle → 400 via handler
        } catch (IOException e) {
            log.error("Failed to store category icon: {}", e.getMessage());
            throw new RuntimeException("Failed to store icon: " + e.getMessage(), e);
        }
    }

    private String resolveExtension(MultipartFile file) {
        // 1. Essayer l'extension du nom de fichier original
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.matches("\\.(png|jpg|jpeg|svg|webp)")) {
                return ext;
            }
        }
        // 2. Fallback : dériver du Content-Type
        String contentType = file.getContentType();
        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "image/png":  return ".png";
                case "image/jpeg": return ".jpg";
                case "image/svg+xml": return ".svg";
                case "image/webp": return ".webp";
                default: break;
            }
        }
        // 3. Défaut sécurisé
        return ".png";
    }

    private String storeFile(MultipartFile file, String subDir, Long partnerId) {
        if (isGcsStorage()) {
            return storeFileInGcs(file, subDir, partnerId, null);
        }
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }

            // Get original extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Generate unique filename: partnerId_uuid.ext
            String filename = partnerId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // Store file
            Path targetDir = Paths.get(uploadDir, subDir);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("File stored successfully: {}", targetPath.toAbsolutePath());

            // Return public URL
            return baseUrl + "/" + subDir + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file for partner {}: {}", partnerId, e.getMessage());
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    private String storeFileWithSuffix(MultipartFile file, String subDir, Long partnerId, String suffix) {
        if (isGcsStorage()) {
            return storeFileInGcs(file, subDir, partnerId, suffix);
        }
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = partnerId + "_" + suffix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path targetDir = Paths.get(uploadDir, subDir);
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully: {}", targetPath.toAbsolutePath());
            return baseUrl + "/" + subDir + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file for partner {}: {}", partnerId, e.getMessage());
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Supprimer un fichier par son URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;

        if (isGcsStorage()) {
            deleteFileFromGcs(fileUrl);
            return;
        }

        try {
            // Extract relative path from URL
            String relativePath = fileUrl.replace(baseUrl + "/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", fileUrl, e.getMessage());
        }
    }

    private boolean isGcsStorage() {
        return STORAGE_TYPE_GCS.equalsIgnoreCase(storageType);
    }

    private String storeFileInGcs(MultipartFile file, String subDir, Long partnerId, String suffix) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Cannot store empty file");
            }
            if (gcsBucket == null || gcsBucket.isBlank()) {
                throw new IllegalStateException("file.gcs.bucket must be configured when file.storage.type=gcs");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String sanitizedSubDir = subDir.startsWith("/") ? subDir.substring(1) : subDir;
            String filename = suffix == null || suffix.isBlank()
                    ? partnerId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension
                    : partnerId + "_" + suffix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            String objectName = sanitizedSubDir + "/" + filename;

            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(gcsBucket, objectName))
                    .setContentType(resolveContentType(file, extension))
                    .build();

            try (InputStream inputStream = file.getInputStream()) {
                storage.createFrom(blobInfo, inputStream);
            }

            String publicUrl = "https://storage.googleapis.com/" + gcsBucket + "/" + objectName;
            log.info("File stored successfully in GCS: bucket={} object={}", gcsBucket, objectName);
            return publicUrl;
        } catch (IOException e) {
            log.error("Failed to store file in GCS for partner {}: {}", partnerId, e.getMessage());
            throw new RuntimeException("Failed to store file in GCS: " + e.getMessage(), e);
        }
    }

    private String resolveContentType(MultipartFile file, String extension) {
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            return file.getContentType();
        }
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".svg" -> "image/svg+xml";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    private void deleteFileFromGcs(String fileUrl) {
        try {
            String publicPrefix = "https://storage.googleapis.com/" + gcsBucket + "/";
            if (!fileUrl.startsWith(publicPrefix)) {
                log.warn("Skipping GCS delete for unsupported URL format: {}", fileUrl);
                return;
            }

            String objectName = fileUrl.substring(publicPrefix.length());
            boolean deleted = storage.delete(BlobId.of(gcsBucket, objectName));
            if (deleted) {
                log.info("Deleted GCS object bucket={} object={}", gcsBucket, objectName);
            }
        } catch (Exception e) {
            log.warn("Failed to delete GCS file {}: {}", fileUrl, e.getMessage());
        }
    }
}
