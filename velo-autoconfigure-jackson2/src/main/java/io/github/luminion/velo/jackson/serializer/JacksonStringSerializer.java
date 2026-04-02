package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.luminion.velo.core.spi.JsonProcessorProvider;
import io.github.luminion.velo.jackson.annotation.JsonEncode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

/**
 * 统一字符串序列化处理器（工厂分发器）
 *
 * @author luminion
 */
@Slf4j
public class JacksonStringSerializer extends StdSerializer<String> implements ContextualSerializer {

    private final JsonProcessorProvider jsonProcessorProvider;

    public JacksonStringSerializer(JsonProcessorProvider jsonProcessorProvider) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(value);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }
        JsonEncode jsonEncode = property.getAnnotation(JsonEncode.class);
        if (jsonEncode != null) {
            Function<String, String> processor = jsonProcessorProvider.getProcessor(jsonEncode.value());
            return new JsonStringFunctionSerializer(processor);
        }
        return this;
    }

    /**
     * 内部执行器：负责具体的函数式序列化
     */
    private static class JsonStringFunctionSerializer extends StdSerializer<String> {
        private final Function<String, String> function;

        public JsonStringFunctionSerializer(Function<String, String> function) {
            super(String.class);
            this.function = function;
        }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            String result = (function != null) ? function.apply(value) : value;
            if (result == null) {
                gen.writeNull();
            } else {
                gen.writeString(result);
            }
        }
    }
}
