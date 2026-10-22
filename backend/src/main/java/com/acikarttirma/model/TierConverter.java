package com.acikarttirma.model;

import com.acikarttirma.model.enums.Tier;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Tier enum'unu SMALLINT (1, 2, 3) olarak saklar. Alanlarda @Convert ile açıkça kullanılır. */
@Converter
public class TierConverter implements AttributeConverter<Tier, Short> {

    @Override
    public Short convertToDatabaseColumn(Tier tier) {
        return tier == null ? null : tier.getValue();
    }

    @Override
    public Tier convertToEntityAttribute(Short value) {
        return value == null ? null : Tier.fromValue(value);
    }
}