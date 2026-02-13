package io.github.luminion.starter.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.luminion.starter.core.annotation.JsonUnmask;
import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.XssIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.function.Function;

/**
 * 统一字符串反序列化处理器
 * 仅处理 String 字段。顺序：1. JsonUnmask -> 2. XSS 清理
 *
 * @author luminion
 */
@Slf4j
public class JacksonStringDeserializer extends StdDeserializer<String> implements ContextualDeserializer {

    private final XssCleaner xssCleaner;
    private final Function<String, String> decodeFunc;
    private final boolean ignoreXss;
    private final ApplicationContext applicationContext;

    public JacksonStringDeserializer(XssCleaner xssCleaner, ApplicationContext applicationContext) {
        this(xssCleaner, null, false, applicationContext);
    }

    private JacksonStringDeserializer(XssCleaner xssCleaner, Function<String, String> decodeFunc, boolean ignoreXss,
                                      ApplicationContext applicationContext) {
        super(String.class);
        this.xssCleaner = xssCleaner;
        this.decodeFunc = decodeFunc;
        this.ignoreXss = ignoreXss;
        this.applicationContext = applicationContext;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null) {
            return null;
        }

        // 1. 执行解码逻辑
        if (decodeFunc != null) {
            text = decodeFunc.apply(text);
        }

        // 2. 执行 XSS 清理
        if (xssCleaner != null && !ignoreXss) {
            text = xssCleaner.clean(text);
        }

        return text;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }

        boolean currentIgnoreXss = property.getAnnotation(XssIgnore.class) != null;
        JsonUnmask decodeAnn = property.getAnnotation(JsonUnmask.class);
        Function<String, String> currentDecodeFunc = null;

        if (decodeAnn != null) {
            Class<? extends Function<String, String>> funcClass = decodeAnn.value();
            try {
                currentDecodeFunc = applicationContext.getBean(funcClass);
            } catch (Exception e) {
                String errorMsg = String.format("未发现 @JsonUnmask 指定的函数类 [%s] 的 Bean 实例。", funcClass.getName());
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }

        // 只有在真的需要特殊处理时，才返回定制化的实例
        if (currentIgnoreXss != this.ignoreXss || currentDecodeFunc != null) {
            return new JacksonStringDeserializer(xssCleaner, currentDecodeFunc, currentIgnoreXss, applicationContext);
        }

        return this;
    }
}
