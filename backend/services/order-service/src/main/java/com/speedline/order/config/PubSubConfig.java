package com.speedline.order.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.ProjectTopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Crée le topic {@code order-events} dans l'émulateur Pub/Sub local (même principe que
 * {@code partner-service} pour {@code partner-events}).
 * En cloud (emulator-host vide), aucune création — Terraform gère les topics.
 */
@Configuration
@Slf4j
public class PubSubConfig {

    @Value("${spring.cloud.gcp.project-id:speedline-local}")
    private String projectId;

    @Value("${spring.cloud.gcp.pubsub.emulator-host:}")
    private String emulatorHost;

    private static final String[] TOPICS = {
        "order-events"
    };

    @PostConstruct
    public void initializePubSub() {
        String normalized = emulatorHost == null ? "" : emulatorHost.trim();
        try {
            log.info("[order-service PubSubConfig] Project ID: {}", projectId);
            if (!normalized.isBlank()) {
                log.info("[order-service PubSubConfig] Mode EMULATOR — création des topics: {}", (Object) TOPICS);
                createTopicsForEmulator(normalized);
            } else {
                log.info("[order-service PubSubConfig] Mode GCP réel — pas de bootstrap des topics (Terraform)");
            }
        } catch (Exception e) {
            log.warn("[order-service PubSubConfig] Bootstrap échoué: {}", e.getMessage(), e);
        }
    }

    private void createTopicsForEmulator(String normalizedEmulatorHost) throws Exception {
        String[] hostPort = normalizedEmulatorHost.split(":");
        if (hostPort.length != 2) {
            throw new IllegalArgumentException("PUBSUB_EMULATOR_HOST attendu host:port, reçu: " + normalizedEmulatorHost);
        }
        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(channel));
        CredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();

        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            for (String topicName : TOPICS) {
                createTopicIfMissing(topicAdminClient, topicName);
            }
        } finally {
            channel.shutdown();
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        }
    }

    private void createTopicIfMissing(TopicAdminClient topicAdminClient, String topicName) {
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);
            try {
                topicAdminClient.getTopic(projectTopicName);
                log.info("[order-service PubSubConfig] Topic déjà existant: {}", topicName);
            } catch (Exception ignored) {
                topicAdminClient.createTopic(projectTopicName);
                log.info("[order-service PubSubConfig] Topic créé: {}", topicName);
            }
        } catch (Exception e) {
            log.error("[order-service PubSubConfig] Topic {} : {}", topicName, e.getMessage(), e);
        }
    }
}
