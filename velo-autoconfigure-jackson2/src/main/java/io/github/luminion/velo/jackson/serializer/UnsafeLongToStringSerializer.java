package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class UnsafeLongToStringSerializer extends JsonSerializer<Long> {

    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value.longValue() > MAX_SAFE_INTEGER || value.longValue() < -MAX_SAFE_INTEGER) {
            gen.writeString(Long.toString(value));
            return;
        }
        gen.writeNumber(value.longValue());
    }
}
