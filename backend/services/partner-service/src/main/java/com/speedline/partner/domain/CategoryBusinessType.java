package com.speedline.partner.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CategoryBusinessType { // ✅ Majuscule
    RESTAURANT,
    GROCERY,
    PHARMACY,
    OTHER;

    @JsonCreator
    public static CategoryBusinessType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Le type métier est requis");
        }
        for (CategoryBusinessType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException(
                "Type métier invalide: '" + value + "'. Valeurs acceptées: RESTAURANT, GROCERY, PHARMACY, OTHER"
        );
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}