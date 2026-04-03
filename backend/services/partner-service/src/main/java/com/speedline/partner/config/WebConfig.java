package com.speedline.partner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration pour servir les fichiers uploadés comme ressources statiques
 * Les fichiers dans ./uploads/ sont accessibles via /uploads/**
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.storage.type:local}")
    private String storageType;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path configuredUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path workspaceUploadPath = Paths.get("uploads").toAbsolutePath().normalize();
        Path backendUploadPath = Paths.get("backend", "uploads").toAbsolutePath().normalize();

        // Primary media path used by current APIs
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        toFileLocation(configuredUploadPath),
                        toFileLocation(workspaceUploadPath),
                        toFileLocation(backendUploadPath)
                );

        // Backward-compatibility for legacy URLs stored without /uploads prefix
        registry.addResourceHandler("/partners/**")
                .addResourceLocations(
                        toFileLocation(configuredUploadPath.resolve("partners")),
                        toFileLocation(workspaceUploadPath.resolve("partners")),
                        toFileLocation(backendUploadPath.resolve("partners"))
                );

        registry.addResourceHandler("/categories/**")
                .addResourceLocations(
                        toFileLocation(configuredUploadPath.resolve("categories")),
                        toFileLocation(workspaceUploadPath.resolve("categories")),
                        toFileLocation(backendUploadPath.resolve("categories"))
                );
    }

    private String toFileLocation(Path directory) {
        // Uri form is cross-platform safe (Windows/Linux) and preserves absolute paths.
        return directory.toUri().toString();
    }
}
