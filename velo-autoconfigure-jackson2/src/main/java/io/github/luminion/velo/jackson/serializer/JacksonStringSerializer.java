package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import io.github.luminion.velo.jackson.annotation.JsonEncode;
import io.github.luminion.velo.spi.JsonProcessorProvider;

import java.io.IOException;
import java.util.function.Function;

/**
 * 按属性应用 @JsonEncode，字符串写出与类型处理委托 Jackson 原生实现。
 *
 * @author luminion
 */
public class JacksonStringSerializer extends StdSerializer<String> implements ContextualSerializer {

    /** 委托原生字符串序列化，保留 Jackson 的转义与类型处理行为。 */
    private static final StringSerializer STRING_SERIALIZER = new StringSerializer();

    /** 提供按属性注解创建字符串转换器的能力。 */
    private final JsonProcessorProvider jsonProcessorProvider;

    /** 当前属性对应的字符串转换函数；为空时直接写出原字符串。 */
    private final Function<String, String> function;

    /** 创建未绑定具体属性转换函数的字符串序列化器。 */
    public JacksonStringSerializer(JsonProcessorProvider jsonProcessorProvider) {
        this(jsonProcessorProvider, null);
    }

    private JacksonStringSerializer(JsonProcessorProvider jsonProcessorProvider, Function<String, String> function) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
        this.function = function;
    }

    /** 应用属性转换后委托 Jackson 原生字符串序列化器写出结果。 */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String result = encode(value);
        if (result == null) {
            gen.writeNull();
            return;
        }
        STRING_SERIALIZER.serialize(result, gen, provider);
    }

    /** 应用属性转换，并由 Jackson 原生序列化器保留多态类型处理。 */
    @Override
    public void serializeWithType(String value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSerializer) throws IOException {
        String result = encode(value);
        if (result == null) {
            gen.writeNull();
            return;
        }
        // 自然字符串的类型处理交给 Jackson，避免自行写入多态包装。
        STRING_SERIALIZER.serializeWithType(result, gen, provider, typeSerializer);
    }

    /** 按 Jackson 原生字符串规则判断属性值是否为空。 */
    @Override
    public boolean isEmpty(SerializerProvider provider, String value) {
        return value == null || STRING_SERIALIZER.isEmpty(provider, value);
    }

    private String encode(String value) {
        return value == null || function == null ? value : function.apply(value);
    }

    /** 根据属性上的编码注解创建属性级字符串序列化器。 */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return STRING_SERIALIZER;
        }
        JsonEncode jsonEncode = property.getAnnotation(JsonEncode.class);
        if (jsonEncode != null) {
            Function<String, String> processor = jsonProcessorProvider.getProcessor(jsonEncode.value());
            return processor == null ? STRING_SERIALIZER : new JacksonStringSerializer(jsonProcessorProvider, processor);
        }
        return STRING_SERIALIZER;
    }
}
