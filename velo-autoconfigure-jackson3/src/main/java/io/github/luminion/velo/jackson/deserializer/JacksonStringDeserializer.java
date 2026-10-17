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
import tools.jackson.databind.deser.jdk.StringDeserializer;

import java.util.function.Function;

/** 根据属性应用 {@code @JsonDecode} 和 XSS 清洗的 Jackson 3 字符串反序列化器。 */
public class JacksonStringDeserializer extends StdDeserializer<String> {

    /** JSON 字符串处理器提供者。 */
    private final JsonProcessorProvider jsonProcessorProvider;

    /** 可选的 XSS 清洗器。 */
    private final XssCleaner xssCleaner;

    /**
     * 创建字符串反序列化器。
     *
     * @param jsonProcessorProvider JSON 字符串处理器提供者
     * @param xssCleaner 可选的 XSS 清洗器
     */
    public JacksonStringDeserializer(JsonProcessorProvider jsonProcessorProvider, XssCleaner xssCleaner) {
        super(String.class);
        this.jsonProcessorProvider = jsonProcessorProvider;
        this.xssCleaner = xssCleaner;
    }

    /** 将空值按 Jackson 的 {@code AS_EMPTY} 规则处理为空字符串。 */
    @Override
    public String getEmptyValue(DeserializationContext context) {
        return "";
    }

    /**
     * 先使用 Jackson 原生规则读取字符串，再执行全局启用的 XSS 清洗。
     */
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        // 先遵循 Jackson 的 token 与 coercion 规则，再应用字符串增强。
        String text = StringDeserializer.instance.deserialize(p, ctxt);
        if (text != null && !text.isEmpty() && xssCleaner != null) {
            return xssCleaner.clean(text);
        }
        return text;
    }

    /**
     * 根据属性注解创建针对当前属性的字符串处理器。
     */
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

    /** 应用当前属性组合后的字符串转换函数。 */
    private static class JsonStringFunctionDeserializer extends StdDeserializer<String> {

        /** 当前属性的转换函数。 */
        private final Function<String, String> function;

        /**
         * 创建属性级字符串反序列化器。
         *
         * @param function 属性对应的转换函数
         */
        JsonStringFunctionDeserializer(Function<String, String> function) {
            super(String.class);
            this.function = function;
        }

        /** 将空值按 Jackson 的 {@code AS_EMPTY} 规则处理为空字符串。 */
        @Override
        public String getEmptyValue(DeserializationContext context) {
            return "";
        }

        /** 先由 Jackson 解析，再应用属性级转换函数。 */
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            // 先遵循 Jackson 的 token 与 coercion 规则，再应用字符串增强。
            String text = StringDeserializer.instance.deserialize(p, ctxt);
            if (text == null || text.isEmpty()) {
                return text;
            }
            return function == null ? text : function.apply(text);
        }
    }
}
