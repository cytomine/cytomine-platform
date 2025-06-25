package be.cytomine.converter;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;
@Converter
public class UUIDConverter implements AttributeConverter<UUID, String> {
    @Override
    public String convertToDatabaseColumn(UUID attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public UUID convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UUID.fromString(dbData);
    }
}
