package io.github.luminion.starter.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.github.luminion.starter.core.spi.EnumFieldConvention;
import io.github.luminion.starter.jackson.annotation.JsonEnum;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jackson 序列化增强：自动基于约定翻译枚举字段
 *
 * @author luminion
 * @since 1.0.0
 */
public class JsonEnumSerializerModifier extends BeanSerializerModifier {

    private final EnumFieldConvention enumFieldConvention;
    private final Map<String, EnumMetadata> metadataCache = new ConcurrentHashMap<>();

    public JsonEnumSerializerModifier(EnumFieldConvention enumFieldConvention) {
        this.enumFieldConvention = enumFieldConvention;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {
        List<BeanPropertyWriter> newProperties = new ArrayList<>(beanProperties);
        Set<String> existNames = new HashSet<>();
        for (BeanPropertyWriter writer : beanProperties) {
            existNames.add(writer.getName());
        }

        for (BeanPropertyWriter writer : beanProperties) {
            JsonEnum ann = writer.getAnnotation(JsonEnum.class);
            if (ann == null) {
                continue;
            }

            String targetName = writer.getName() + "name";
            if (existNames.contains(targetName)) {
                continue;
            }

            EnumMetadata metadata = getMetadata(ann, writer.getType().getRawClass());
            if (metadata != null) {
                newProperties.add(new JsonEnumPropertyWriter(writer, targetName, metadata));
            }
        }
        return newProperties;
    }

    private EnumMetadata getMetadata(JsonEnum ann, Class<?> fieldType) {
        Class<? extends Enum<?>> enumClass = ann.value();
        String cacheKey = enumClass.getName() + ":" + fieldType.getName() + ":" + ann.keyField() + ":"
                + ann.labelField();

        return metadataCache.computeIfAbsent(cacheKey, k -> {
            Field kField = findKeyField(enumClass, ann.keyField(), fieldType);
            Field vField = findLabelField(enumClass, ann.labelField());

            if (kField == null || vField == null) {
                return null;
            }

            Map<Object, Object> mapping = new HashMap<>();
            Enum<?>[] constants = enumClass.getEnumConstants();
            for (Enum<?> constant : constants) {
                Object key = getFieldValue(constant, kField);
                Object label = getFieldValue(constant, vField);
                if (key != null && label != null) {
                    mapping.put(key, label);
                }
            }
            return new EnumMetadata(mapping);
        });
    }

    private Field findKeyField(Class<?> clazz, String explicitName, Class<?> fieldType) {
        if (StringUtils.hasText(explicitName)) {
            Field f = ReflectionUtils.findField(clazz, explicitName);
            if (f != null) {
                ReflectionUtils.makeAccessible(f);
                return f;
            }
        }
        // 使用 EnumFieldConvention 提供的约定
        List<String> candidates = enumFieldConvention.codeFieldNames();
        for (String name : candidates) {
            Field f = ReflectionUtils.findField(clazz, name);
            if (f != null) {
                ReflectionUtils.makeAccessible(f);
                return f;
            }
        }
        // 类型匹配兜底
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getType().isAssignableFrom(fieldType) && !f.isEnumConstant() && !f.isSynthetic()) {
                ReflectionUtils.makeAccessible(f);
                return f;
            }
        }
        return null;
    }

    private Field findLabelField(Class<?> clazz, String explicitName) {
        if (StringUtils.hasText(explicitName)) {
            Field f = ReflectionUtils.findField(clazz, explicitName);
            if (f != null) {
                ReflectionUtils.makeAccessible(f);
                return f;
            }
        }
        // 使用 EnumFieldConvention 提供的约定
        List<String> candidates = enumFieldConvention.descFieldNames();
        for (String name : candidates) {
            Field f = ReflectionUtils.findField(clazz, name);
            if (f != null && f.getType() == String.class) {
                ReflectionUtils.makeAccessible(f);
                return f;
            }
        }
        return null;
    }

    private Object getFieldValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private static class EnumMetadata {
        private final Map<Object, Object> mapping;

        public EnumMetadata(Map<Object, Object> mapping) {
            this.mapping = mapping;
        }

        public Object getLabel(Object key) {
            return mapping.get(key);
        }
    }

    private static class JsonEnumPropertyWriter extends BeanPropertyWriter {
        private final String targetName;
        private final EnumMetadata metadata;

        public JsonEnumPropertyWriter(BeanPropertyWriter base, String targetName, EnumMetadata metadata) {
            super(base, new PropertyName(targetName));
            this.targetName = targetName;
            this.metadata = metadata;
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
            Object value = getMember().getValue(bean);
            if (value != null) {
                Object label = metadata.getLabel(value);
                if (label != null) {
                    gen.writeStringField(targetName, label.toString());
                }
            }
        }
    }
}
