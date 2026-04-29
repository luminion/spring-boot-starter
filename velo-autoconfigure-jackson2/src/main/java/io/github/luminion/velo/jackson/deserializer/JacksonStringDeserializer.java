package io.github.luminion.velo.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.jackson.annotation.JsonDecode;
import io.github.luminion.velo.xss.XssCleaner;
import io.github.luminion.velo.xss.XssIgnore;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.function.Function;

/**
 * 统一字符串反序列化处理器（工厂分发器）
 *
 * @author luminion
 */
@Slf4j
public class JacksonStringDeserializer extends StdDeserializer<String> implements ContextualDeserializer {
    private final JsonProcessorProvider jsonProcessorProvider;
    private final XssCleaner xssCleaner;

    public JacksonStringDeserializer(JsonProcessorProvider jsonProcessorProvider, XssCleaner xssCleaner) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
        this.xssCleaner = xssCleaner;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text != null && !text.isEmpty() && xssCleaner != null) {
            return xssCleaner.clean(text);
        }
        return text;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }

        boolean ignoreXss = property.getAnnotation(XssIgnore.class) != null;
        JsonDecode jsonDecode = property.getAnnotation(JsonDecode.class);

        if (!ignoreXss && jsonDecode == null) {
            return this;
        }
        Function<String, String> compositeFunc = jsonDecode == null ? t -> t : jsonProcessorProvider.getProcessor(jsonDecode.value());
        if (xssCleaner != null && !ignoreXss) {
            Function<String, String> processor = compositeFunc == null ? t -> t : compositeFunc;
            compositeFunc = processor.andThen(xssCleaner::clean);
        }
        return new JsonStringFunctionDeserializer(compositeFunc);
    }

    /**
     * 内部执行器：负责具体的函数式反序列化
     */
    private static class JsonStringFunctionDeserializer extends StdDeserializer<String> {
        private final Function<String, String> function;

        public JsonStringFunctionDeserializer(Function<String, String> function) {
            super(String.class);
            this.function = function;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            if (text == null || text.isEmpty()) {
                return text;
            }
            return (function != null) ? function.apply(text) : text;
        }
    }
}
