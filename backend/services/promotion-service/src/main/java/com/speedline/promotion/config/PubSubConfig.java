package com.speedline.promotion.config;

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
 * Bootstrap Pub/Sub topics and subscriptions for the promotion service.
 * Only creates local emulator resources when PUBSUB_EMULATOR_HOST is set.
 * Production resources are managed by Terraform.
 */
@Configuration
@Slf4j
public class PubSubConfig {

    @Value("${spring.cloud.gcp.project-id:speedline-local}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.emulator-host:${PUBSUB_EMULATOR_HOST:${SPRING_CLOUD_GCP_PUBSUB_EMULATOR_HOST:}}}")
    private String emulatorHost;

    /** topic -> subscription pairs owned by this service */
    private static final String[][] TOPIC_SUBSCRIPTIONS = {
        {"promotion-created",    "promotion-created-promotion-sub"},
        {"promotion-applied",    "promotion-applied-promotion-sub"},
        {"promotion-revoked",    "promotion-revoked-promotion-sub"},
        {"promotion-expired",    "promotion-expired-promotion-sub"},
        {"promotion-activated",  "promotion-activated-promotion-sub"},
        {"order-cancelled",      "order-cancelled-promotion-sub"},
        {"user-registered",      "user-registered-promotion-sub"}
    };

    @PostConstruct
    public void initializePubSub() {
        String host = emulatorHost == null ? "" : emulatorHost.trim();
        try {
            if (!isValid(host)) {
                log.info("[PubSubConfig] Emulator host not set → skipping local bootstrap (real GCP / Terraform)");
                return;
            }
            log.info("[PubSubConfig] Bootstrapping emulator at {}", host);
            bootstrap(host);
            log.info("[PubSubConfig] Emulator bootstrap complete");
        } catch (Exception e) {
            log.warn("[PubSubConfig] Emulator bootstrap failed (non-fatal): {}", e.getMessage());
        }
    }

    private boolean isValid(String host) {
        if (host.isBlank() || !host.contains(":")) return false;
        String[] parts = host.split(":", 2);
        if (parts[0].isBlank() || parts[1].isBlank()) return false;
        try {
            int port = Integer.parseInt(parts[1]);
            return port > 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void bootstrap(String host) throws Exception {
        String[] parts = host.split(":", 2);
        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
            GrpcTransportChannel.create(
                ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1])).usePlaintext().build()
            )
        );
        CredentialsProvider credentials = NoCredentialsProvider.create();

        try (TopicAdminClient topics = TopicAdminClient.create(
                 TopicAdminSettings.newBuilder()
                     .setTransportChannelProvider(channelProvider)
                     .setCredentialsProvider(credentials).build());
             SubscriptionAdminClient subs = SubscriptionAdminClient.create(
                 SubscriptionAdminSettings.newBuilder()
                     .setTransportChannelProvider(channelProvider)
                     .setCredentialsProvider(credentials).build())) {

            for (String[] entry : TOPIC_SUBSCRIPTIONS) {
                ensureTopicAndSubscription(topics, subs, entry[0], entry[1]);
            }
        }
    }

    private void ensureTopicAndSubscription(TopicAdminClient topics, SubscriptionAdminClient subs,
                                             String topicName, String subName) {
        String topicPath = ProjectTopicName.of(projectId, topicName).toString();
        try {
            topics.getTopic(topicPath);
            log.debug("Topic '{}' already exists", topicName);
        } catch (Exception e) {
            topics.createTopic(topicPath);
            log.info("Created topic: {}", topicName);
        }

        String subPath = ProjectSubscriptionName.of(projectId, subName).toString();
        try {
            subs.getSubscription(subPath);
            log.debug("Subscription '{}' already exists", subName);
        } catch (Exception e) {
            subs.createSubscription(
                Subscription.newBuilder()
                    .setName(subPath)
                    .setTopic(topicPath)
                    .setPushConfig(PushConfig.getDefaultInstance())
                    .setAckDeadlineSeconds(10)
                    .build()
            );
            log.info("Created subscription: {}", subName);
        }
    }
}
