package com.speedline.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication(exclude = {
    com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.storage.GcpStorageAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.datastore.GcpDatastoreAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.spanner.GcpSpannerAutoConfiguration.class
})
@EnableIntegration
@EnableDiscoveryClient
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
