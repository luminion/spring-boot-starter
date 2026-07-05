package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 将 {@link BigInteger} 无条件序列化为字符串。
 * <p>
 * 与 {@link LongToStringSerializer} 行为保持一致：只要开启 {@code serialize-long-as-string}，
 * Long 与 BigInteger 都恒定输出字符串，避免同类型字段因数值大小时而是 number、时而是 string，
 * 保证前端拿到的 JSON 契约稳定。
 */
public class BigIntegerToStringSerializer extends JsonSerializer<BigInteger> {

    @Override
    public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(value.toString());
    }
}
