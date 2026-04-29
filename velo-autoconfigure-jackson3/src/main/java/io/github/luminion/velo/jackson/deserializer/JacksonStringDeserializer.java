package io.github.luminion.velo.jackson.deserializer;

import io.github.luminion.velo.jackson.annotation.JsonDecode;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.xss.XssCleaner;
import io.github.luminion.velo.xss.XssIgnore;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.function.Function;

/**
 * Jackson 3 string deserializer that applies @JsonDecode and XSS cleaning per property.
 */
public class JacksonStringDeserializer extends StdDeserializer<String> {

    private final JsonProcessorProvider jsonProcessorProvider;
    private final XssCleaner xssCleaner;

    public JacksonStringDeserializer(JsonProcessorProvider jsonProcessorProvider, XssCleaner xssCleaner) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
        this.xssCleaner = xssCleaner;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String text = p.getValueAsString();
        if (text != null && !text.isEmpty() && xssCleaner != null) {
            return xssCleaner.clean(text);
        }
        return text;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }

        boolean ignoreXss = property.getAnnotation(XssIgnore.class) != null;
        JsonDecode jsonDecode = property.getAnnotation(JsonDecode.class);
        if (!ignoreXss && jsonDecode == null) {
            return this;
        }

        Function<String, String> compositeFunc = jsonDecode == null ? value -> value
                : jsonProcessorProvider.getProcessor(jsonDecode.value());
        if (xssCleaner != null && !ignoreXss) {
            Function<String, String> processor = compositeFunc == null ? value -> value : compositeFunc;
            compositeFunc = processor.andThen(xssCleaner::clean);
        }
        return new JsonStringFunctionDeserializer(compositeFunc);
    }

    private static class JsonStringFunctionDeserializer extends StdDeserializer<String> {
        private final Function<String, String> function;

        JsonStringFunctionDeserializer(Function<String, String> function) {
            super(String.class);
            this.function = function;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            String text = p.getValueAsString();
            if (text == null || text.isEmpty()) {
                return text;
            }
            return function == null ? text : function.apply(text);
        }
    }
}
