package io.github.luminion.autoconfigure.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
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
public class JsonFieldSerializer extends JsonSerializer<Object> implements ContextualSerializer {
    private static final Map<Class<?>, JsonSerializer<?>> FUNC_CACHE_MAP = new ConcurrentHashMap<>();
    private static final Map<String, JsonSerializer<?>> METHOD_SIGNATURE_CACHE_MAP = new ConcurrentHashMap<>();

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString());
    }


    @Override
    @SneakyThrows
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        JsonMask annotation = property.getAnnotation(JsonMask.class);
        if (annotation != null) {
            JsonSerializer<?> handler = match(annotation, property);
            if (handler != null) {
                return handler;
            }
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    @SneakyThrows
    public JsonSerializer<?> match(JsonMask annotation, BeanProperty property) {
        Class<?> propertyRawClass = property.getType().getRawClass();
        String methodFullName = property.getMember().getFullName();
        Class<? extends Function<?, String>> funcClass = annotation.serialize();
        JsonSerializer<?> cache = METHOD_SIGNATURE_CACHE_MAP.get(methodFullName);
        if (funcClass.equals(JsonMask.Fallback.class)) {
            return null;
        }
        if (cache != null) {
            return FiledSerializer.DEFAULT.equals(cache) ? null : cache;
        }
        // 泛型检查
        Class<?>[] resolveTypeArguments = GenericTypeResolver.resolveTypeArguments(funcClass, Function.class);
        if (resolveTypeArguments == null || resolveTypeArguments.length != 2) {
            METHOD_SIGNATURE_CACHE_MAP.put(methodFullName, FiledSerializer.DEFAULT);
            log.warn("no generic type args in {}", funcClass.getName());
            return null;
        }
        Class<?> argClass = resolveTypeArguments[0];
        Class<?> returnClass = resolveTypeArguments[1];
        if (!String.class.equals(returnClass) || !propertyRawClass.isAssignableFrom(argClass)) {
            METHOD_SIGNATURE_CACHE_MAP.put(methodFullName, FiledSerializer.DEFAULT);
            log.warn("type not match, method: {}, required: Function<{}, {}>, provided: Function<{}, {}>"
                    , methodFullName
                    , propertyRawClass.getName()
                    , String.class.getName()
                    , argClass.getName()
                    , returnClass.getName()
            );
            return null;
        }

        // 缓存
        JsonSerializer<?> handler = FUNC_CACHE_MAP.get(funcClass);
        if (handler == null) {
            Function<?, String> function = funcClass.getConstructor().newInstance();
            handler = new FiledSerializer<>(function);
        }
        FUNC_CACHE_MAP.put(funcClass, handler);
        METHOD_SIGNATURE_CACHE_MAP.put(methodFullName, handler);
        return handler;
    }


    public static class FiledSerializer<T> extends JsonSerializer<T> {
        private final Function<T, String> func;
        public static final FiledSerializer<String> DEFAULT = new FiledSerializer<>(t -> t);

        public FiledSerializer(Function<T, String> func) {
            this.func = func;
        }

        @Override
        public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(func.apply(value));
        }
    }


}
