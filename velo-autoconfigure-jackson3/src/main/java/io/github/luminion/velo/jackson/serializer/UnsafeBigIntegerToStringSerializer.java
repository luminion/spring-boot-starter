package io.github.luminion.velo.jackson.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigInteger;

public class UnsafeBigIntegerToStringSerializer extends StdSerializer<BigInteger> {

    private static final BigInteger MAX_SAFE_INTEGER = BigInteger.valueOf(9007199254740991L);

    public UnsafeBigIntegerToStringSerializer() {
        super(BigInteger.class);
    }

    @Override
    public void serialize(BigInteger value, JsonGenerator gen, SerializationContext ctxt) {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value.abs().compareTo(MAX_SAFE_INTEGER) > 0) {
            gen.writeString(value.toString());
            return;
        }
        gen.writeNumber(value);
    }
}
