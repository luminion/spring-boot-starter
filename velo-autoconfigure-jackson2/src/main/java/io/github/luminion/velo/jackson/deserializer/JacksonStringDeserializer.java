package io.github.luminion.velo.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
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
    public String getEmptyValue(DeserializationContext context) {
        return "";
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 先遵循 Jackson 的 token 与 coercion 规则，再应用字符串增强。
        String text = readString(p, ctxt);
        if (text != null && !text.isEmpty() && xssCleaner != null) {
            return xssCleaner.clean(text);
        }
        return text;
    }

    private static String readString(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        CoercionInputShape shape = null;
        if (token == JsonToken.VALUE_NUMBER_INT) {
            shape = CoercionInputShape.Integer;
        } else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
            shape = CoercionInputShape.Float;
        } else if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) {
            shape = CoercionInputShape.Boolean;
        }
        // Jackson 2.13 的原生 StringDeserializer 尚未完整应用标量 coercion，补齐显式规则。
        if (shape != null) {
            CoercionAction action = context.findCoercionAction(LogicalType.Textual, String.class, shape);
            if (action == CoercionAction.Fail) {
                return context.reportInputMismatch(String.class, "Cannot coerce %s value to String", shape);
            }
            if (action == CoercionAction.AsNull) {
                return null;
            }
            if (action == CoercionAction.AsEmpty) {
                return "";
            }
        }
        return StringDeserializer.instance.deserialize(parser, context);
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
        public String getEmptyValue(DeserializationContext context) {
            return "";
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 先遵循 Jackson 的 token 与 coercion 规则，再应用字符串增强。
            String text = readString(p, ctxt);
            if (text == null || text.isEmpty()) {
                return text;
            }
            return (function != null) ? function.apply(text) : text;
        }
    }
}
