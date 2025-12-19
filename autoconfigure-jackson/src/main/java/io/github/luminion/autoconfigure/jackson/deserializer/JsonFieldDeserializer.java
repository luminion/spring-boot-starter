package io.github.luminion.autoconfigure.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import io.github.luminion.autoconfigure.jackson.annotation.JsonMask;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.GenericTypeResolver;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author luminion
 */
@Slf4j
public class JsonFieldDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
    private static final Map<Class<?>, JsonDeserializer<?>> FUNC_CACHE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, JsonDeserializer<?>> METHOD_SIGNATURE_CACHE_MAP = new ConcurrentHashMap<>();

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        return null;
    }


    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JsonMask annotation = property.getAnnotation(JsonMask.class);
        if (annotation != null) {
            JsonDeserializer<?> handler = match(annotation, property);
            if (handler != null) {
                return handler;
            }
        }
        return ctxt.findContextualValueDeserializer(property.getType(), property);
    }


    @SneakyThrows
    public JsonDeserializer<?> match(JsonMask annotation, BeanProperty property) {
        Class<?> propertyRawClass = property.getType().getRawClass();
        String methodFullName = property.getMember().getFullName();
        Class<? extends Function<String, ?>> funcClass = annotation.deserialize();
        JsonDeserializer<?> cache = METHOD_SIGNATURE_CACHE_MAP.get(methodFullName);
        if (funcClass.equals(JsonMask.Fallback.class)) {
            return null;
        }
        if (cache != null) {
            return FiledDeserializer.DEFAULT.equals(cache) ? null : cache;
        }

        // 泛型检查
        Class<?>[] resolveTypeArguments = GenericTypeResolver.resolveTypeArguments(funcClass, Function.class);
        if (resolveTypeArguments == null || resolveTypeArguments.length != 2) {
            METHOD_SIGNATURE_CACHE_MAP.put(methodFullName, FiledDeserializer.DEFAULT);
            log.warn("no generic type args in {}",funcClass.getName());
            return null;
        }
        Class<?> argClass = resolveTypeArguments[0];
        Class<?> returnClass = resolveTypeArguments[1];
        if (!String.class.equals(argClass) || !propertyRawClass.isAssignableFrom(returnClass)) {
            METHOD_SIGNATURE_CACHE_MAP.put(methodFullName, FiledDeserializer.DEFAULT);
            log.warn("type not match, method: {}, required: Function<{}, {}>, provided: Function<{}, {}>"
                    , methodFullName
                    , String.class.getName()
                    , propertyRawClass.getName()
                    , argClass.getName()
                    , returnClass.getName()
            );
            return null;
        }

        // 缓存
        JsonDeserializer<?> handler = FUNC_CACHE_MAP.get(funcClass);
        if (handler == null) {
            Function<String, ?> function = funcClass.getConstructor().newInstance();
            handler = new FiledDeserializer<>(function);
        }
        FUNC_CACHE_MAP.put(funcClass, handler);
        METHOD_SIGNATURE_CACHE_MAP.put(methodFullName, handler);
        return handler;
    }


    public static class FiledDeserializer<T> extends JsonDeserializer<T> {
        private final Function<String, T> func;
        public static final FiledDeserializer<String> DEFAULT = new FiledDeserializer<>(t -> t);

        public FiledDeserializer(Function<String, T> func) {
            this.func = func;
        }

        @Override
        public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return func.apply(p.getValueAsString());
        }
    }


}
