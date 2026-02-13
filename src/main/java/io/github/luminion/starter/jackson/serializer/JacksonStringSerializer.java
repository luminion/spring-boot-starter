package io.github.luminion.starter.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.luminion.starter.core.annotation.JsonMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.function.Function;

/**
 * 统一字符串序列化处理器
 * 仅处理 String 字段。用于处理 @JsonMask 注解（如脱敏）
 *
 * @author luminion
 */
@Slf4j
public class JacksonStringSerializer extends StdSerializer<String> implements ContextualSerializer {

    private final Function<String, String> encodeFunc;
    private final ApplicationContext applicationContext;

    public JacksonStringSerializer(ApplicationContext applicationContext) {
        this(null, applicationContext);
    }

    private JacksonStringSerializer(Function<String, String> encodeFunc, ApplicationContext applicationContext) {
        super(String.class);
        this.encodeFunc = encodeFunc;
        this.applicationContext = applicationContext;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        if (encodeFunc != null) {
            String result = encodeFunc.apply(value);
            if (result == null) {
                gen.writeNull();
            } else {
                gen.writeString(result);
            }
        } else {
            // 默认行为：直接写出原字符串
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return this;
        }

        JsonMask encodeAnn = property.getAnnotation(JsonMask.class);
        if (encodeAnn != null) {
            Class<? extends Function<String, String>> funcClass = encodeAnn.value();
            Function<String, String> currentEncodeFunc = null;
            try {
                // 强制从 Spring 容器获取
                currentEncodeFunc = applicationContext.getBean(funcClass);
            } catch (Exception e) {
                String errorMsg = String.format("未发现 @JsonMask 指定的函数类 [%s] 的 Bean 实例。", funcClass.getName());
                log.error(errorMsg);
                throw new RuntimeException(errorMsg, e);
            }
            if (currentEncodeFunc != null) {
                return new JacksonStringSerializer(currentEncodeFunc, applicationContext);
            }
        }

        return this;
    }
}
