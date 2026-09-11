package io.github.luminion.velo.webflux;

import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import io.github.luminion.velo.util.InvocationUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 WebFlux 实际消息写入器的运行时 JSON 序列化器。
 *
 * <p>不直接依赖 Jackson 2 或 Jackson 3，而是使用应用最终配置的 WebFlux 编解码器，
 * 因此 Boot 2/3/4 及两套 Jackson 实现都可以复用同一套日志序列化逻辑。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
public class WebFluxRuntimeJsonSerializer implements RuntimeJsonSerializer {

    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    private static final String OMITTED_VALUE = "[omitted]";

    private static final String CIRCULAR_VALUE = "[circular]";

    private final List<HttpMessageWriter<?>> writers;

    public WebFluxRuntimeJsonSerializer(List<HttpMessageWriter<?>> writers) {
        this.writers = writers == null ? Collections.emptyList() : new ArrayList<>(writers);
    }

    @Override
    public String toJson(Object value) {
        Object jsonValue = sanitizeValue(value, new IdentityHashMap<>());
        if (jsonValue == null) {
            return "null";
        }
        if (jsonValue instanceof CharSequence || jsonValue instanceof Character) {
            return "\"" + escapeJson(jsonValue.toString()) + "\"";
        }

        ResolvableType valueType = ResolvableType.forClass(jsonValue.getClass());
        for (HttpMessageWriter<?> writer : writers) {
            if (!(writer instanceof EncoderHttpMessageWriter<?>) || !writer.canWrite(valueType, JSON)) {
                continue;
            }
            try {
                return encode((EncoderHttpMessageWriter<?>) writer, jsonValue, valueType);
            } catch (RuntimeException ex) {
                return fallback(jsonValue);
            }
        }
        return fallback(jsonValue);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String encode(EncoderHttpMessageWriter<?> writer, Object value, ResolvableType valueType) {
        Encoder encoder = writer.getEncoder();
        DataBuffer dataBuffer = encoder.encodeValue(value, DefaultDataBufferFactory.sharedInstance,
                valueType, JSON, Collections.emptyMap());
        if (dataBuffer == null) {
            return fallback(value);
        }
        try {
            ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }

    private String fallback(Object value) {
        return "\"" + escapeJson(InvocationUtils.formatValue(value, 0)) + "\"";
    }

    private Object sanitizeValue(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) {
            return null;
        }
        Class<?> valueType = value.getClass();
        if (valueType.isArray()) {
            if (visited.containsKey(value)) {
                return CIRCULAR_VALUE;
            }
            visited.put(value, Boolean.TRUE);
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(sanitizeValue(Array.get(value, i), visited));
            }
            visited.remove(value);
            return values;
        }
        if (value instanceof Collection<?>) {
            if (visited.containsKey(value)) {
                return CIRCULAR_VALUE;
            }
            visited.put(value, Boolean.TRUE);
            Collection<?> collection = (Collection<?>) value;
            List<Object> values = new ArrayList<>(collection.size());
            for (Object item : collection) {
                values.add(sanitizeValue(item, visited));
            }
            visited.remove(value);
            return values;
        }
        if (value instanceof Map<?, ?>) {
            if (visited.containsKey(value)) {
                return CIRCULAR_VALUE;
            }
            visited.put(value, Boolean.TRUE);
            Map<?, ?> map = (Map<?, ?>) value;
            Map<Object, Object> values = new LinkedHashMap<>(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                values.put(sanitizeValue(entry.getKey(), visited), sanitizeValue(entry.getValue(), visited));
            }
            visited.remove(value);
            return values;
        }
        if (!InvocationUtils.isLoggableValue(value)) {
            return OMITTED_VALUE;
        }
        return value;
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
            }
        }
        return builder.toString();
    }
}
