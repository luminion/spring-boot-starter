package io.github.luminion.velo.jackson.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;

public class ConfigurableBigDecimalSerializer extends StdSerializer<BigDecimal> {

    private final boolean asString;

    private final boolean stripTrailingZeros;

    public ConfigurableBigDecimalSerializer(boolean asString, boolean stripTrailingZeros) {
        super(BigDecimal.class);
        this.asString = asString;
        this.stripTrailingZeros = stripTrailingZeros;
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) {
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
