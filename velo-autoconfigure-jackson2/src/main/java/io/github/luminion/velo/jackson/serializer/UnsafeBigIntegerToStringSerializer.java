package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigInteger;

public class UnsafeBigIntegerToStringSerializer extends JsonSerializer<BigInteger> {

    private static final BigInteger MAX_SAFE_INTEGER = BigInteger.valueOf(9007199254740991L);

    @Override
    public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
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
