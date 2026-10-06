package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jdk.NumberSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.ser.std.ToStringSerializerBase;

import java.math.BigDecimal;

public class ConfigurableBigDecimalSerializer extends StdSerializer<BigDecimal> {

    private final boolean asString;

    private final boolean stripTrailingZeros;

    private final ValueSerializer<? super BigDecimal> delegate;

    public ConfigurableBigDecimalSerializer(boolean asString, boolean stripTrailingZeros) {
        super(BigDecimal.class);
        this.asString = asString;
        this.stripTrailingZeros = stripTrailingZeros;
        this.delegate = asString ? new PlainBigDecimalSerializer() : new NumberSerializer(BigDecimal.class);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
        JsonFormat.Value format = findFormatOverrides(context, property, BigDecimal.class);
        boolean resolved = asString;
        if (format != null && format.getShape() != JsonFormat.Shape.ANY) {
            resolved = format.getShape() == JsonFormat.Shape.STRING;
        }
        return resolved == asString ? this : new ConfigurableBigDecimalSerializer(resolved, stripTrailingZeros);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serialize(normalized, gen, ctxt);
    }

    @Override
    public void serializeWithType(BigDecimal value, JsonGenerator generator, SerializationContext context,
                                  TypeSerializer typeSerializer) {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serializeWithType(normalized, generator, context, typeSerializer);
    }

    private static class PlainBigDecimalSerializer extends ToStringSerializerBase {
        PlainBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public String valueToString(Object value) {
            return ((BigDecimal) value).toPlainString();
        }
    }
}
