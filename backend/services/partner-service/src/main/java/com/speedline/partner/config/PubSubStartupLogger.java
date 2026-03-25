package com.speedline.partner.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logs Pub/Sub configuration at startup to provide clear visibility into which mode is active.
 * Helps developers quickly identify misconfiguration issues.
 */
@Component
@Slf4j
public class PubSubStartupLogger {

    @Value("${spring.cloud.gcp.project-id:}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.emulator-host:}")
    private String emulatorHost;

    private final Environment environment;

    public PubSubStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logStartupConfiguration() {
        String activeProfiles = Arrays.toString(environment.getActiveProfiles());
        if (activeProfiles.isEmpty() || activeProfiles.equals("[]")) {
            activeProfiles = "default (local)";
        }

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              PUB/SUB CONFIGURATION AT STARTUP                   ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Active Profiles: {}", String.format("%-47s║", activeProfiles));
        log.info("║ GCP Project ID:  {}", String.format("%-47s║", projectId));
        
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            log.info("║ Pub/Sub Mode:    {}", String.format("%-47s║", "EMULATOR (localhost)"));
            log.info("║ Emulator Host:   {}", String.format("%-47s║", emulatorHost));
            log.info("║ Bootstrap:       {}", String.format("%-47s║", "Creates topics/subscriptions locally"));
        } else {
            log.info("║ Pub/Sub Mode:    {}", String.format("%-47s║", "REAL GCP (Cloud Run)"));
            log.info("║ Emulator Host:   {}", String.format("%-47s║", "NOT SET (using real GCP)"));
            log.info("║ Bootstrap:       {}", String.format("%-47s║", "Managed by Terraform"));
        }
        
        log.info("║                                                                ║");
        log.info("║ RECOMMENDATION:                                                ║");
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            log.info("║ • Ensure Docker Compose pubsub-emulator service is running:  ║");
            log.info("║   docker-compose up -d pubsub-emulator                       ║");
            log.info("║ • Topics/subscriptions created automatically on first call   ║");
        } else {
            log.info("║ • Ensure Workload Identity is configured on Cloud Run        ║");
            log.info("║ • Service account must have pubsub.publisher/subscriber role ║");
            log.info("║ • Topics/subscriptions managed by Terraform                 ║");
        }
        
        log.info("╚════════════════════════════════════════════════════════════════╝");
        
        // Validate configuration consistency
        validateConfiguration();
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted() {
        // Log application fully started
        log.info("Partner Service started successfully with Pub/Sub configured");
    }

    private void validateConfiguration() {
        String activeProfile = getActiveProfile();
        
        if ("local".equals(activeProfile) || activeProfile.isEmpty()) {
            if (emulatorHost == null || emulatorHost.isEmpty()) {
                log.warn("⚠️  LOCAL PROFILE but emulator-host is NOT set!");
                log.warn("    Set PUBSUB_EMULATOR_HOST=localhost:8090 or run with profile 'local'");
            }
        }
        
        if ("dev".equals(activeProfile)) {
            if (emulatorHost != null && !emulatorHost.isEmpty()) {
                log.warn("⚠️  DEV PROFILE but emulator-host IS SET: {}", emulatorHost);
                log.warn("    This will connect to local emulator instead of real GCP Pub/Sub!");
                log.warn("    Remove PUBSUB_EMULATOR_HOST environment variable");
            }
        }
    }

    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles != null && profiles.length > 0) {
            return profiles[0];
        }
        return "";
    }
}
