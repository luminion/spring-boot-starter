package io.github.luminion.starter.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.luminion.starter.mask.annotation.Mask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.function.Function;

/**
 * 统一字符串序列化处理器（工厂分发器）
 *
 * @author luminion
 */
@Slf4j
public class JacksonStringSerializer extends StdSerializer<String> implements ContextualSerializer {

    private final ApplicationContext applicationContext;

    public JacksonStringSerializer(ApplicationContext applicationContext) {
        super(String.class);
        this.applicationContext = applicationContext;
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
        Mask maskAnn = property.getAnnotation(Mask.class);
        if (maskAnn != null) {
            Class<? extends Function<String, String>> funcClass = maskAnn.value();
            try {
                Function<String, String> masker = applicationContext.getBean(funcClass);
                return new JsonStringFunctionSerializer(masker);
            } catch (Exception e) {
                String errorMsg = String.format("未发现 @Mask 指定的函数类 [%s] 的 Bean 实例。", funcClass.getName());
                log.error(errorMsg);
                throw new JsonMappingException(prov.getGenerator(), errorMsg, e);
            }
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
