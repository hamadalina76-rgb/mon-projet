package com.speedline.partner.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.CachingPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.util.List;

/**
 * DEV Profile Configuration for Pub/Sub.
 * Activates when Spring profile "dev" is active.
 *
 * In DEV mode:
 * - Uses real Google Cloud Pub/Sub (no emulator)
 * - Credentials provided by Workload Identity
 * - Topics/subscriptions managed by Terraform
 */
@Configuration
@Profile("dev")
@Slf4j
public class PubSubDevConfiguration {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    public PubSubDevConfiguration() {
        log.info("✅ PubSubDevConfiguration loaded - Using REAL GCP Pub/Sub");
    }

    /**
     * Force explicit ADC usage in Cloud Run instead of relying on any implicit
     * provider selection inside the Pub/Sub auto-configuration chain.
     */
    @Bean
    @Primary
    public CredentialsProvider pubsubCredentialsProvider() throws IOException {
        GoogleCredentials credentials =
                GoogleCredentials.getApplicationDefault()
                        .createScoped(List.of("https://www.googleapis.com/auth/pubsub"));
        credentials.refreshIfExpired();

        log.info("✅ Injecting REAL GCP FixedCredentialsProvider backed by ADC.");
        return FixedCredentialsProvider.create(credentials);
    }

    /**
     * Override the exact bean names expected by Spring Cloud GCP auto-config so
     * the publisher path uses the production gRPC channel.
     */
    @Bean
    public TransportChannelProvider publisherTransportChannelProvider() {
        log.info("✅ Injecting REAL GCP publisher transport channel.");
        return PublisherStubSettings.defaultGrpcTransportProviderBuilder().build();
    }

    @Bean
    public TransportChannelProvider subscriberTransportChannelProvider() {
        log.info("✅ Injecting REAL GCP subscriber transport channel.");
        return SubscriberStubSettings.defaultGrpcTransportProviderBuilder().build();
    }

    @Bean
    @Primary
    public PublisherFactory publisherFactory(
            CredentialsProvider pubsubCredentialsProvider,
            @Qualifier("publisherTransportChannelProvider")
            TransportChannelProvider publisherTransportChannelProvider) {
        GcpProjectIdProvider projectIdProvider = () -> projectId;

        DefaultPublisherFactory delegate = new DefaultPublisherFactory(projectIdProvider);
        delegate.setCredentialsProvider(pubsubCredentialsProvider);
        delegate.setChannelProvider(publisherTransportChannelProvider);

        log.info("✅ Using explicit DefaultPublisherFactory with ADC credentials for project {}", projectId);
        return new CachingPublisherFactory(delegate);
    }

    @Bean
    @Primary
    public SubscriberFactory subscriberFactory(
            CredentialsProvider pubsubCredentialsProvider,
            @Qualifier("subscriberTransportChannelProvider")
            TransportChannelProvider subscriberTransportChannelProvider) {
        GcpProjectIdProvider projectIdProvider = () -> projectId;

        PubSubConfiguration pubSubConfiguration = new PubSubConfiguration();
        pubSubConfiguration.initialize(projectId);

        DefaultSubscriberFactory factory = new DefaultSubscriberFactory(projectIdProvider, pubSubConfiguration);
        factory.setCredentialsProvider(pubsubCredentialsProvider);
        factory.setChannelProvider(subscriberTransportChannelProvider);
        factory.setMaxAckExtensionPeriod(Duration.ofSeconds(600));

        log.info("✅ Using explicit DefaultSubscriberFactory with ADC credentials for project {}", projectId);
        return factory;
    }

    @Bean
    @Primary
    public PubSubTemplate pubSubTemplate(
            PublisherFactory publisherFactory,
            SubscriberFactory subscriberFactory) {
        log.info("✅ Using explicit primary PubSubTemplate backed by custom factories.");
        return new PubSubTemplate(publisherFactory, subscriberFactory);
    }
}
