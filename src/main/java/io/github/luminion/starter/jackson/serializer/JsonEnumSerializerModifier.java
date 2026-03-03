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

            Class<? extends Enum<?>> enumClass = ann.value();
            Class<?> fieldType = writer.getType().getRawClass();

            // 1. 根据字段类型判断: 属性名或约定中的 codeField 必须存在且类型相同
            Field keyField = findKeyField(enumClass, ann.keyField(), fieldType);
            if (keyField == null) {
                continue;
            }

            // 2. 获取匹配的描述字段 (descFieldNames)
            Field labelField = findLabelField(enumClass, ann.labelField());
            if (labelField == null) {
                continue;
            }

            // 3. 额外字段名计算: 原字段名 + 翻译属性名(首字母大写)
            // 例如: status + desc (字段) -> statusDesc
            String labelFieldName = labelField.getName();
            String targetName = writer.getName() + StringUtils.capitalize(labelFieldName);

            // 4. 确保没有重复字段
            if (existNames.contains(targetName)) {
                continue;
            }

            EnumMetadata metadata = getMetadata(enumClass, keyField, labelField);
            if (metadata != null) {
                newProperties.add(new JsonEnumPropertyWriter(writer, targetName, metadata));
                existNames.add(targetName); // 防止本次循环添加重复
            }
        }
        return newProperties;
    }

    private EnumMetadata getMetadata(Class<? extends Enum<?>> enumClass, Field keyField, Field labelField) {
        String cacheKey = enumClass.getName() + ":" + keyField.getName() + ":" + labelField.getName();

        return metadataCache.computeIfAbsent(cacheKey, k -> {
            Map<Object, Object> mapping = new HashMap<>();
            Enum<?>[] constants = enumClass.getEnumConstants();
            for (Enum<?> constant : constants) {
                Object key = getFieldValue(constant, keyField);
                Object label = getFieldValue(constant, labelField);
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

        List<String> candidates = enumFieldConvention.codeFieldNames();
        for (String name : candidates) {
            Field f = ReflectionUtils.findField(clazz, name);
            if (f != null) {
                // 类型相同判断 (考虑基本类型装饰类)
                if (isTypeCompatible(f.getType(), fieldType)) {
                    ReflectionUtils.makeAccessible(f);
                    return f;
                }
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

        List<String> candidates = enumFieldConvention.descFieldNames();
        for (String name : candidates) {
            Field f = ReflectionUtils.findField(clazz, name);
            if (f != null) {
                ReflectionUtils.makeAccessible(f);
                return f;
            }
        }
        return null;
    }

    private boolean isTypeCompatible(Class<?> enumFieldType, Class<?> propertyType) {
        if (enumFieldType.isAssignableFrom(propertyType)) {
            return true;
        }
        // 简单处理基本类型
        if (enumFieldType.isPrimitive()) {
            return getWrapperType(enumFieldType).isAssignableFrom(propertyType);
        }
        if (propertyType.isPrimitive()) {
            return enumFieldType.isAssignableFrom(getWrapperType(propertyType));
        }
        return false;
    }

    private Class<?> getWrapperType(Class<?> primitive) {
        if (primitive == int.class)
            return Integer.class;
        if (primitive == long.class)
            return Long.class;
        if (primitive == boolean.class)
            return Boolean.class;
        if (primitive == double.class)
            return Double.class;
        if (primitive == float.class)
            return Float.class;
        if (primitive == byte.class)
            return Byte.class;
        if (primitive == char.class)
            return Character.class;
        if (primitive == short.class)
            return Short.class;
        return primitive;
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
