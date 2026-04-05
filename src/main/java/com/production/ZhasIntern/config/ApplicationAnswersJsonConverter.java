package com.production.ZhasIntern.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Converter
public class ApplicationAnswersJsonConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        Map<String, Object> safeAttribute = attribute == null ? Map.of() : attribute;
        try {
            return OBJECT_MAPPER.writeValueAsString(safeAttribute);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Cannot serialize application answers to JSON", ex);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new HashMap<>();
        }

        try {
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(dbData, MAP_TYPE);
            return parsed == null ? new HashMap<>() : new HashMap<>(parsed);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Cannot deserialize application answers from JSON", ex);
        }
    }
}
