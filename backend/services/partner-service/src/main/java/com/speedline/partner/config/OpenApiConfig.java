package com.speedline.partner.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI partnerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpeedLine Partner Service API")
                        .description("API de gestion des partenaires (établissements), horaires, staff et statut.")
                        .version("1.0.0"));
    }
}
