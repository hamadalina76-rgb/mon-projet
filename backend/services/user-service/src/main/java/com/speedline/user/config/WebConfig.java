package com.speedline.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration pour servir les fichiers statiques (photos de profil)
 * Configuration pour servir les fichiers statiques uploadés
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir:uploads/profile-pictures}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convert to absolute path and normalize
        java.io.File uploadDirFile = new java.io.File(uploadDir);
        String absolutePath = uploadDirFile.getAbsolutePath();

        // Ensure path ends with separator for proper resource location
        String resourceLocation = "file:" + absolutePath;
        if (!absolutePath.endsWith("/") && !absolutePath.endsWith("\\")) {
            resourceLocation += "/";
        }

        // Servir les fichiers uploadés depuis /uploads/**
        // Les fichiers seront accessibles via: http://localhost:8082/uploads/couriers/123/file.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600); // Cache de 1 heure

    }
}
