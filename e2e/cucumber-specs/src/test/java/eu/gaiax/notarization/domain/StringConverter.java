package eu.gaiax.notarization.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 *
 * @author Florian Otto
 */

@Converter
public class StringConverter implements AttributeConverter<String, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        return attribute == null ? new byte[0] : attribute.getBytes();
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        return new String(dbData);
    }

}
