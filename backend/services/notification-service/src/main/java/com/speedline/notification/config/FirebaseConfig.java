package com.speedline.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes Firebase App for FCM (optional: only if credentials are provided).
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (FirebaseApp.getApps() != null && !FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase App already initialized");
            return;
        }
        try {
            InputStream stream = getCredentialsStream();
            if (stream == null) {
                log.warn("Firebase credentials not configured. FCM push will be disabled. Set firebase.credentials-path or GOOGLE_APPLICATION_CREDENTIALS.");
                return;
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase App initialized successfully for FCM");
        } catch (IOException e) {
            log.warn("Could not initialize Firebase (FCM disabled): {}", e.getMessage());
        }
    }

    private InputStream getCredentialsStream() throws IOException {
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            String path = credentialsPath.trim();
            // classpath:credeentials/file.json -> charge depuis src/main/resources
            if (path.startsWith("classpath:")) {
                String resourcePath = path.substring("classpath:".length()).trim();
                return new ClassPathResource(resourcePath).getInputStream();
            }
            return new FileInputStream(path);
        }
        String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (envPath != null && !envPath.isBlank()) {
            return new FileInputStream(envPath);
        }
        return null;
    }
}
