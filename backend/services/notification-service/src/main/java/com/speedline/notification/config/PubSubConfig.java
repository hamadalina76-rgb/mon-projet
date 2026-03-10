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
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
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
 * Configuration for GCP Pub/Sub topics and subscriptions.
 * Creates topics/subscriptions if they don't exist (works with emulator and real GCP).
 */
@Configuration
@Slf4j
public class PubSubConfig {

    @Value("${spring.cloud.gcp.project-id:speedline-local}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.emulator-host:}")
    private String emulatorHost;

    @PostConstruct
    public void initializePubSub() {
        try {
            log.info("Initializing Pub/Sub topics and subscriptions for project: {}", projectId);
            
            if (emulatorHost != null && !emulatorHost.isEmpty()) {
                log.info("Using Pub/Sub emulator at: {}", emulatorHost);
                createTopicsAndSubscriptionsForEmulator();
            } else {
                log.info("Using real GCP Pub/Sub (no emulator)");
                createTopicsAndSubscriptions();
            }
            
            log.info("Pub/Sub initialization completed successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize Pub/Sub (may already exist or emulator not running): {}", e.getMessage());
        }
    }

    private void createTopicsAndSubscriptionsForEmulator() throws Exception {
        String[] hostPort = emulatorHost.split(":");
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

    private void createTopicsAndSubscriptions() throws Exception {
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create();
             SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {

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

