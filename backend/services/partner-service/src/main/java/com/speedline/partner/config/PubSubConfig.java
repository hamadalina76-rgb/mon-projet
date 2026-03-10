package com.speedline.partner.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.Topic;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for GCP Pub/Sub topics.
 * Creates topics if they don't exist (works with emulator and real GCP).
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
            log.info("Initializing Pub/Sub topics for project: {}", projectId);
            
            if (emulatorHost != null && !emulatorHost.isEmpty()) {
                log.info("Using Pub/Sub emulator at: {}", emulatorHost);
                createTopicsForEmulator();
            } else {
                log.info("Using real GCP Pub/Sub (no emulator)");
                createTopics();
            }
            
            log.info("Pub/Sub topics initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize Pub/Sub topics (may already exist or emulator not running): {}", e.getMessage());
        }
    }

    private void createTopicsForEmulator() throws Exception {
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

        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(credentialsProvider)
            .build();

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            createTopic(topicAdminClient, "partner-events");
            createTopic(topicAdminClient, "partner-product-stock");
            createTopic(topicAdminClient, "partner-promotion-ending");
        }
    }

    private void createTopics() throws Exception {
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create()) {
            createTopic(topicAdminClient, "partner-events");
            createTopic(topicAdminClient, "partner-product-stock");
            createTopic(topicAdminClient, "partner-promotion-ending");
        }
    }

    private void createTopic(TopicAdminClient topicAdminClient, String topicName) {
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            
            try {
                Topic topic = topicAdminClient.getTopic(projectTopicName);
                log.info("Topic '{}' already exists", topicName);
            } catch (Exception e) {
                Topic topic = topicAdminClient.createTopic(projectTopicName);
                log.info("Created topic: {}", topicName);
            }
        } catch (Exception e) {
            log.error("Failed to create topic {}: {}", topicName, e.getMessage());
        }
    }
}
