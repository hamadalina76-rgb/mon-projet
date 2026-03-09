package com.speedline.partner.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonNameI18nConverter {

    private final ObjectMapper objectMapper;

    /**
     * Convertir Map en String JSON
     */
    public String toJson(Map<String, String> nameMap) throws JsonProcessingException {
        return objectMapper.writeValueAsString(nameMap);
    }

    /**
     * Convertir String JSON en Map
     */
    public Map<String, String> toMap(String jsonString) throws JsonProcessingException {
        if (jsonString == null || jsonString.isEmpty()) {
            return new HashMap<>();  // Retourne une carte vide si le JSON est vide
        }
        return objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {});  // Convertir la chaîne JSON en Map
    }

    /**
     * Extraire un nom par locale
     */
    public String getNameByLocale(String jsonString, String locale) throws JsonProcessingException {
        Map<String, String> nameMap = toMap(jsonString);
        return nameMap.getOrDefault(locale.toLowerCase(), nameMap.get("fr"));
    }

    /**
     * Vérifier si une locale existe dans le JSON
     */
    public boolean hasLocale(String jsonString, String locale) throws JsonProcessingException {
        Map<String, String> nameMap = toMap(jsonString);
        return nameMap.containsKey(locale.toLowerCase());
    }
}