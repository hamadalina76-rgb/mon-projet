package com.speedline.partner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.storage.GcpStorageAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.datastore.GcpDatastoreAutoConfiguration.class,
    com.google.cloud.spring.autoconfigure.spanner.GcpSpannerAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class PartnerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PartnerServiceApplication.class, args);
    }
}
