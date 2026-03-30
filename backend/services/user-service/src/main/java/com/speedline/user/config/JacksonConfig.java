package com.speedline.user.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.io.IOException;

/**
 * Override the broken Sort serializer registered by Spring Cloud OpenFeign's
 * SortJsonComponent, which wraps IOException in EncodeException and crashes
 * Spring MVC response serialization whenever a Page<T> is returned directly.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer sortSerializerCustomizer() {
        return builder -> builder.serializerByType(Sort.class, new JsonSerializer<Sort>() {
            @Override
            public void serialize(Sort value, JsonGenerator gen, SerializerProvider serializers)
                    throws IOException {
                gen.writeStartArray();
                for (Sort.Order order : value) {
                    gen.writeString(order.getProperty() + ": " + order.getDirection().name());
                }
                gen.writeEndArray();
            }
        });
    }
}
