package io.github.luminion.velo.jackson.serializer;

import io.github.luminion.velo.jackson.annotation.JsonEncode;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.util.function.Function;

/**
 * Jackson 3 string serializer that applies @JsonEncode on a per-property basis.
 */
public class JacksonStringSerializer extends StdSerializer<String> {

    private final JsonProcessorProvider jsonProcessorProvider;

    public JacksonStringSerializer(JsonProcessorProvider jsonProcessorProvider) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeString(value);
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }
        JsonEncode jsonEncode = property.getAnnotation(JsonEncode.class);
        if (jsonEncode == null) {
            return this;
        }
        return new JsonStringFunctionSerializer(jsonProcessorProvider.getProcessor(jsonEncode.value()));
    }

    private static class JsonStringFunctionSerializer extends StdSerializer<String> {
        private final Function<String, String> function;

        JsonStringFunctionSerializer(Function<String, String> function) {
            super(String.class);
            this.function = function;
        }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
            String result = function == null ? value : function.apply(value);
            if (result == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(result);
        }
    }
}
