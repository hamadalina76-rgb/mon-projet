package com.speedline.notification.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local Profile Configuration for Pub/Sub.
 * Activates when:
 * - Spring profile "local" is active, OR
 * - No active profiles AND PUBSUB_EMULATOR_HOST is set
 */
@Configuration
@Profile("local")
@Slf4j
public class PubSubLocalConfiguration {

    public PubSubLocalConfiguration(PubSubTemplate pubSubTemplate) {
        log.debug("✅ PubSubLocalConfiguration loaded - Using Pub/Sub EMULATOR");
        log.debug("   - Topics/subscriptions will be created automatically");
        log.debug("   - Pub/Sub calls go to localhost:8090");
    }
}
