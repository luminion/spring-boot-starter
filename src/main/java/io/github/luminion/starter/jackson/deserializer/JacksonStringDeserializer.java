package io.github.luminion.starter.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.luminion.starter.core.annotation.Unmask;
import io.github.luminion.starter.xss.XssCleaner;
import io.github.luminion.starter.xss.XssIgnore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.function.Function;

/**
 * 统一字符串反序列化处理器（工厂分发器）
 *
 * @author luminion
 */
@Slf4j
public class JacksonStringDeserializer extends StdDeserializer<String> implements ContextualDeserializer {
    private final ApplicationContext applicationContext;
    private final XssCleaner xssCleaner;

    public JacksonStringDeserializer(ApplicationContext applicationContext) {
        super(String.class);
        this.applicationContext = applicationContext;
        this.xssCleaner = applicationContext.getBeanProvider(XssCleaner.class).getIfAvailable();
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
        Unmask unmaskAnn = property.getAnnotation(Unmask.class);

        if (!ignoreXss && unmaskAnn == null) {
            return this;
        }

        Function<String, String> compositeFunc = null;

        if (unmaskAnn != null) {
            Class<? extends Function<String, String>> funcClass = unmaskAnn.value();
            try {
                compositeFunc = applicationContext.getBean(funcClass);
            } catch (Exception e) {
                String errorMsg = String.format("未发现 @Unmask 指定的函数类 [%s] 的 Bean 实例。", funcClass.getName());
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg, e);
            }
        }

        if (xssCleaner != null && !ignoreXss) {
            compositeFunc = compositeFunc.andThen(xssCleaner::clean);
        }

        if (compositeFunc == null) {
            return new JsonStringFunctionDeserializer(t -> t);
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
