package io.github.luminion.velo.jackson.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class UnsafeLongToStringSerializer extends StdSerializer<Long> {

    private static final long MAX_SAFE_INTEGER = 9007199254740991L;

    public UnsafeLongToStringSerializer() {
        super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
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
