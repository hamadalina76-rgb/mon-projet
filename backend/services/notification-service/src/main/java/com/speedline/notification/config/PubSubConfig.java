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
import com.google.pubsub.v1.Topic;
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

    @PostConstruct
    public void initializePubSub() {
        try {
            log.info("Initializing Pub/Sub bootstrap for project: {}", projectId);

            if (!isEmulatorExplicitlyConfigured()) {
                log.info("PUBSUB_EMULATOR_HOST not set; skipping emulator bootstrap. Using real GCP Pub/Sub managed by Terraform.");
                return;
            }

            log.info("Using Pub/Sub emulator at: {}", emulatorHost);
            createTopicsAndSubscriptionsForEmulator();
            log.info("Pub/Sub emulator bootstrap completed successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize Pub/Sub emulator bootstrap: {}", e.getMessage());
        }
    }

    private boolean isEmulatorExplicitlyConfigured() {
        if (emulatorHost == null || emulatorHost.isBlank()) {
            return false;
        }

        if (!emulatorHost.contains(":")) {
            log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': expected host:port", emulatorHost);
            return false;
        }

        String[] hostPort = emulatorHost.split(":", 2);
        if (hostPort[0].isBlank() || hostPort[1].isBlank()) {
            log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': expected host:port", emulatorHost);
            return false;
        }

        try {
            int port = Integer.parseInt(hostPort[1]);
            if (port <= 0 || port > 65535) {
                log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': port out of range", emulatorHost);
                return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            log.warn("Ignoring invalid PUBSUB_EMULATOR_HOST '{}': non-numeric port", emulatorHost);
            return false;
        }
    }

    private void createTopicsAndSubscriptionsForEmulator() throws Exception {
        String[] hostPort = emulatorHost.split(":", 2);
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder
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

            // Create partner-events topic and subscription
            createTopicAndSubscription(
                topicAdminClient,
                subscriptionAdminClient,
                "partner-events",
                "partner-events-notification-sub"
            );
            // Create partner-product-stock topic and subscription (stock alerts)
            createTopicAndSubscription(
                topicAdminClient,
                subscriptionAdminClient,
                "partner-product-stock",
                "partner-product-stock-notification-sub"
            );
            // Create partner-promotion-ending topic and subscription (promo ending in 3 days)
            createTopicAndSubscription(
                topicAdminClient,
                subscriptionAdminClient,
                "partner-promotion-ending",
                "partner-promotion-ending-notification-sub"
            );
        }
    }

    private void createTopicAndSubscription(
            TopicAdminClient topicAdminClient,
            SubscriptionAdminClient subscriptionAdminClient,
            String topicName,
            String subscriptionName) {
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            
            // Create topic if not exists
            try {
                Topic topic = topicAdminClient.getTopic(projectTopicName);
                log.info("Topic '{}' already exists", topicName);
            } catch (Exception e) {
                Topic topic = topicAdminClient.createTopic(projectTopicName);
                log.info("Created topic: {}", topicName);
            }

            // Create subscription if not exists
            ProjectSubscriptionName projectSubscriptionName = 
                ProjectSubscriptionName.of(projectId, subscriptionName);
            
            try {
                Subscription sub = subscriptionAdminClient.getSubscription(projectSubscriptionName);
                log.info("Subscription '{}' already exists", subscriptionName);
            } catch (Exception e) {
                Subscription subscription = subscriptionAdminClient.createSubscription(
                    Subscription.newBuilder()
                        .setName(projectSubscriptionName.toString())
                        .setTopic(projectTopicName.toString())
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

