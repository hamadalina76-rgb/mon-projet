package com.speedline.partner.config;

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
 * Configuration de bootstrap Pub/Sub.
 *
 * Logique choisie :
 *
 * 1) En LOCAL (émulateur Pub/Sub actif)
 *    - si spring.cloud.gcp.pubsub.emulator-host est défini et non vide
 *    - alors l'application crée automatiquement les topics dans l'émulateur
 *    - aucun credential GCP réel n'est utilisé
 *
 * 2) En DEV / CLOUD RUN (vrai GCP Pub/Sub)
 *    - si emulator-host est vide ou absent
 *    - alors l'application NE CRÉE RIEN
 *    - les topics/subscriptions sont gérés par Terraform
 *
 * Objectif :
 * - isoler proprement local vs cloud
 * - éviter toute tentative d'administration Pub/Sub côté Cloud Run
 * - éviter les conflits avec l'infra gérée par Terraform
 */
@Configuration
@Slf4j
public class PubSubConfig {

    /**
     * ID du projet GCP utilisé pour Pub/Sub.
     * - local : souvent "speedline-local"
     * - dev   : valeur injectée par application-dev.yml / variable d'env
     */
    @Value("${spring.cloud.gcp.project-id:speedline-local}")
    private String projectId;

    /**
     * Hôte de l'émulateur Pub/Sub.
     * - local : ex "localhost:8090"
     * - dev   : doit être vide ""
     */
    @Value("${spring.cloud.gcp.pubsub.emulator-host:}")
    private String emulatorHost;

    /**
     * Liste centralisée des topics nécessaires au partner-service.
     * En local, ils sont créés automatiquement dans l'émulateur.
     * En dev/cloud, ils doivent déjà exister côté GCP/Terraform.
     */
    private static final String[] TOPICS = {
        "partner-events",
        "partner-product-stock",
        "partner-promotion-ending"
    };

    /**
     * Méthode exécutée automatiquement après l'initialisation du bean Spring.
     *
     * Comportement :
     * - si emulator-host est défini => bootstrap local des topics
     * - sinon => aucun bootstrap (mode cloud réel)
     */
    @PostConstruct
    public void initializePubSub() {
        // Nettoyage de la valeur pour éviter les faux positifs avec espaces
        String normalizedEmulatorHost = emulatorHost == null ? "" : emulatorHost.trim();

        try {
            log.info("==================================================");
            log.info("[PubSubConfig] Initialisation de la configuration Pub/Sub");
            log.info("[PubSubConfig] Project ID : {}", projectId);

            // Cas LOCAL : l'émulateur est défini => on crée les topics localement
            if (!normalizedEmulatorHost.isBlank()) {
                log.info("[PubSubConfig] Mode détecté : LOCAL / EMULATOR");
                log.info("[PubSubConfig] Emulator host : {}", normalizedEmulatorHost);
                log.info("[PubSubConfig] Création automatique des topics dans l'émulateur...");
                createTopicsForEmulator(normalizedEmulatorHost);
                log.info("[PubSubConfig] Bootstrap local Pub/Sub terminé avec succès");
            } else {
                // Cas DEV/CLOUD : aucun emulator => on ne touche pas à l'infra Pub/Sub
                log.info("[PubSubConfig] Mode détecté : REAL GCP / CLOUD RUN");
                log.info("[PubSubConfig] Emulator host : NON DÉFINI");
                log.info("[PubSubConfig] Aucune création de topic/subscription côté application");
                log.info("[PubSubConfig] Les ressources Pub/Sub sont gérées par Terraform");
            }

            log.info("==================================================");
        } catch (Exception e) {
            // Important : on log l'exception complète pour faciliter le debug
            log.warn("[PubSubConfig] Échec du bootstrap Pub/Sub : {}", e.getMessage(), e);
        }
    }

    /**
     * Crée les topics dans l'émulateur Pub/Sub local.
     *
     * Ici :
     * - on ouvre une connexion gRPC vers l'émulateur
     * - on utilise NoCredentialsProvider car l'émulateur n'a pas besoin d'auth GCP
     * - on crée les topics un par un s'ils n'existent pas
     *
     * @param normalizedEmulatorHost hôte:port de l'émulateur, ex "localhost:8090"
     */
    private void createTopicsForEmulator(String normalizedEmulatorHost) throws Exception {
        String[] hostPort = normalizedEmulatorHost.split(":");

        // Validation minimale pour éviter ArrayIndexOutOfBounds ou port invalide
        if (hostPort.length != 2) {
            throw new IllegalArgumentException(
                "Format invalide pour PUBSUB_EMULATOR_HOST. Format attendu : host:port, valeur reçue : "
                    + normalizedEmulatorHost
            );
        }

        String host = hostPort[0];
        int port = Integer.parseInt(hostPort[1]);

        // Construction du canal gRPC vers l'émulateur local
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext()
            .build();

        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(
            GrpcTransportChannel.create(channel)
        );

        // Aucun credential réel nécessaire pour l'émulateur
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

    /**
     * Crée un topic uniquement s'il n'existe pas déjà dans l'émulateur.
     *
     * Logique :
     * - on tente un getTopic(...)
     * - si ça existe déjà => log info
     * - sinon => createTopic(...)
     *
     * @param topicAdminClient client admin Pub/Sub configuré pour l'émulateur
     * @param topicName nom du topic à vérifier/créer
     */
    private void createTopicIfMissing(TopicAdminClient topicAdminClient, String topicName) {
        try {
            ProjectTopicName projectTopicName = ProjectTopicName.of(projectId, topicName);

            try {
                // Vérifie si le topic existe déjà
                topicAdminClient.getTopic(projectTopicName);
                log.info("[PubSubConfig] Topic déjà existant : {}", topicName);
            } catch (Exception getException) {
                // Si le topic n'existe pas, on le crée
                topicAdminClient.createTopic(projectTopicName);
                log.info("[PubSubConfig] Topic créé : {}", topicName);
            }
        } catch (Exception e) {
            // Log complet pour avoir la vraie cause si la création échoue
            log.error("[PubSubConfig] Échec création/vérification du topic {} : {}", topicName, e.getMessage(), e);
        }
    }
}