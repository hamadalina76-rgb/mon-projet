package com.speedline.notification.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for GCP Pub/Sub emulator bootstrap.
 *
 * Runtime infrastructure in Cloud Run (topics/subscriptions/IAM) is managed by Terraform.
 * This class only initializes local emulator resources when emulator host is explicitly provided.
 */
@Configuration
@Slf4j
public class PubSubConfig {

    @Value("${spring.cloud.gcp.project-id:speedline-local}")
    private String projectId;

    // Source of truth: Spring Cloud GCP pubsub emulator-host.
    // application.yml already sets this property (often from env var PUBSUB_EMULATOR_HOST),
    // so we must read it from spring.cloud.gcp.* to reliably bootstrap local resources.
    @Value("${spring.cloud.gcp.pubsub.emulator-host:${PUBSUB_EMULATOR_HOST:${SPRING_CLOUD_GCP_PUBSUB_EMULATOR_HOST:}}}")
    private String emulatorHost;

    private static final String[][] TOPIC_SUBSCRIPTIONS = {
        {"partner-events", "partner-events-notification-sub"},
        {"partner-product-stock", "partner-product-stock-notification-sub"},
        {"partner-promotion-ending", "partner-promotion-ending-notification-sub"}
    };

    @PostConstruct
    public void initializePubSub() {
        String normalizedEmulatorHost = emulatorHost == null ? "" : emulatorHost.trim();

        try {
            log.info("[PubSubConfig] Project ID: {}", projectId);

            if (!isEmulatorExplicitlyConfigured(normalizedEmulatorHost)) {
                log.info("[PubSubConfig] Mode: REAL GCP (DEV/CLOUD RUN)");
                log.info("[PubSubConfig] Emulator host: NOT SET");
                log.info("[PubSubConfig] Topic/subscription creation skipped (managed by Terraform)");
                return;
            }

            log.info("[PubSubConfig] Mode: LOCAL EMULATOR");
            log.info("[PubSubConfig] Emulator host: {}", normalizedEmulatorHost);
            log.info("[PubSubConfig] Bootstrapping topics/subscriptions locally in emulator");
            createTopicsAndSubscriptionsForEmulator(normalizedEmulatorHost);
            log.info("[PubSubConfig] Emulator bootstrap completed successfully");
        } catch (Exception e) {
            log.warn("[PubSubConfig] Emulator bootstrap failed: {}", e.getMessage());
        }
    }

    private boolean isEmulatorExplicitlyConfigured(String normalizedEmulatorHost) {
        if (normalizedEmulatorHost.isBlank()) {
            return false;
        }

        if (!normalizedEmulatorHost.contains(":")) {
            log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': expected host:port", normalizedEmulatorHost);
            return false;
        }

        String[] hostPort = normalizedEmulatorHost.split(":", 2);
        if (hostPort[0].isBlank() || hostPort[1].isBlank()) {
            log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': expected host:port", normalizedEmulatorHost);
            return false;
        }

        try {
            int port = Integer.parseInt(hostPort[1]);
            if (port <= 0 || port > 65535) {
                log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': port out of range", normalizedEmulatorHost);
                return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': non-numeric port", normalizedEmulatorHost);
            return false;
        }
    }

    private void createTopicsAndSubscriptionsForEmulator(String normalizedEmulatorHost) throws Exception {
        String[] hostPort = normalizedEmulatorHost.split(":", 2);
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext();

        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
            GrpcTransportChannel.create(channelBuilder.build())
        );
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        // Create topic admin client
        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();

        // Create subscription admin client
        SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings);
             SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings)) {
            for (String[] entry : TOPIC_SUBSCRIPTIONS) {
                createTopicAndSubscription(
                    topicAdminClient,
                    subscriptionAdminClient,
                    entry[0],
                    entry[1]
                );
            }
        }
    }

    private void createTopicAndSubscription(
            TopicAdminClient topicAdminClient,
            SubscriptionAdminClient subscriptionAdminClient,
            String topicName,
            String subscriptionName) {
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            String topicPath = projectTopicName.toString();
            
            // Create topic if not exists
            try {
                topicAdminClient.getTopic(topicPath);
                log.info("Topic '{}' already exists", topicName);
            } catch (Exception e) {
                topicAdminClient.createTopic(topicPath);
                log.info("Created topic: {}", topicName);
            }

            // Create subscription if not exists
            ProjectSubscriptionName projectSubscriptionName = 
                ProjectSubscriptionName.of(projectId, subscriptionName);
            String subscriptionPath = projectSubscriptionName.toString();
            
            try {
                subscriptionAdminClient.getSubscription(subscriptionPath);
                log.info("Subscription '{}' already exists", subscriptionName);
            } catch (Exception e) {
                subscriptionAdminClient.createSubscription(
                    Subscription.newBuilder()
                        .setName(subscriptionPath)
                        .setTopic(topicPath)
                        .setPushConfig(PushConfig.getDefaultInstance())
                        .setAckDeadlineSeconds(10)
                        .build()
                );
                log.info("Created subscription: {}", subscriptionName);
            }
        } catch (Exception e) {
            log.error("Failed to create topic/subscription {}/{}: {}", topicName, subscriptionName, e.getMessage());
        }
    }
}

