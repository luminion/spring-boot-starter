package io.github.luminion.velo.jackson.serializer;

import io.github.luminion.velo.jackson.annotation.JsonEncode;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jdk.StringSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.function.Function;

/**
 * 按属性应用 @JsonEncode，字符串写出与类型处理委托 Jackson 3 原生实现。
 */
public class JacksonStringSerializer extends StdSerializer<String> {

    private static final StringSerializer STRING_SERIALIZER = StringSerializer.instance;

    private final JsonProcessorProvider jsonProcessorProvider;
    private final Function<String, String> function;

    public JacksonStringSerializer(JsonProcessorProvider jsonProcessorProvider) {
        this(jsonProcessorProvider, null);
    }

    private JacksonStringSerializer(JsonProcessorProvider jsonProcessorProvider, Function<String, String> function) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
        this.function = function;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        String result = encode(value);
        if (result == null) {
            gen.writeNull();
            return;
        }
        STRING_SERIALIZER.serialize(result, gen, ctxt);
    }

    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializationContext ctxt,
                                  TypeSerializer typeSerializer) throws JacksonException {
        String result = encode(value);
        if (result == null) {
            gen.writeNull();
            return;
        }
        // 自然字符串的类型处理交给 Jackson，避免自行写入多态包装。
        STRING_SERIALIZER.serializeWithType(result, gen, ctxt, typeSerializer);
    }

    @Override
    public boolean isEmpty(SerializationContext ctxt, String value) {
        return value == null || STRING_SERIALIZER.isEmpty(ctxt, value);
    }

    private String encode(String value) {
        return value == null || function == null ? value : function.apply(value);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return STRING_SERIALIZER;
        }
        JsonEncode jsonEncode = property.getAnnotation(JsonEncode.class);
        if (jsonEncode == null) {
            return STRING_SERIALIZER;
        }
        Function<String, String> processor = jsonProcessorProvider.getProcessor(jsonEncode.value());
        return processor == null ? STRING_SERIALIZER : new JacksonStringSerializer(jsonProcessorProvider, processor);
    }
}
