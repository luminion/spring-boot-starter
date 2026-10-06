package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase;

import java.io.IOException;
import java.math.BigDecimal;

public class ConfigurableBigDecimalSerializer extends StdSerializer<BigDecimal> implements ContextualSerializer {

    private final boolean asString;

    private final boolean stripTrailingZeros;

    private final JsonSerializer<? super BigDecimal> delegate;

    public ConfigurableBigDecimalSerializer(boolean asString, boolean stripTrailingZeros) {
        super(BigDecimal.class);
        this.asString = asString;
        this.stripTrailingZeros = stripTrailingZeros;
        this.delegate = asString ? new PlainBigDecimalSerializer() : new NumberSerializer(BigDecimal.class);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
            throws JsonMappingException {
        JsonFormat.Value format = findFormatOverrides(provider, property, BigDecimal.class);
        boolean resolved = asString;
        if (format != null && format.getShape() != JsonFormat.Shape.ANY) {
            resolved = format.getShape() == JsonFormat.Shape.STRING;
        }
        return resolved == asString ? this : new ConfigurableBigDecimalSerializer(resolved, stripTrailingZeros);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serialize(normalized, gen, serializers);
    }

    @Override
    public void serializeWithType(BigDecimal value, JsonGenerator generator, SerializerProvider provider,
                                  TypeSerializer typeSerializer) throws IOException {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serializeWithType(normalized, generator, provider, typeSerializer);
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
