package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;

public class ConfigurableBigDecimalSerializer extends JsonSerializer<BigDecimal> {

    private final boolean asString;

    private final boolean stripTrailingZeros;

    public ConfigurableBigDecimalSerializer(boolean asString, boolean stripTrailingZeros) {
        this.asString = asString;
        this.stripTrailingZeros = stripTrailingZeros;
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        if (asString) {
            gen.writeString(normalized.toPlainString());
            return;
        }
        gen.writeNumber(normalized);
    }
}
