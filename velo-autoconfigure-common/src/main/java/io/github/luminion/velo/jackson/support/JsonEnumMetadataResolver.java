package io.github.luminion.velo.jackson.support;

import io.github.luminion.velo.VeloProperties;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves enum code/name fields without depending on a specific Jackson major version.
 */
public class JsonEnumMetadataResolver {

    private final VeloProperties.JacksonProperties jacksonProperties;
    private final Map<String, JsonEnumMetadata> metadataCache = new ConcurrentHashMap<>();

    public JsonEnumMetadataResolver(VeloProperties.JacksonProperties jacksonProperties) {
        this.jacksonProperties = jacksonProperties;
    }

    public JsonEnumMetadata resolve(Class<? extends Enum<?>> enumClass, String explicitCodeField,
                                    String explicitNameField, Class<?> propertyType) {
        Map<String, String> enumMappings = jacksonProperties.getEnumMappings();
        if (enumMappings == null || enumMappings.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, String> enumMapping : enumMappings.entrySet()) {
            String codeFieldName = StringUtils.hasText(explicitCodeField) ? explicitCodeField : enumMapping.getKey();
            String nameFieldName = StringUtils.hasText(explicitNameField) ? explicitNameField : enumMapping.getValue();
            if (!StringUtils.hasText(codeFieldName) || !StringUtils.hasText(nameFieldName)) {
                continue;
            }

            Field codeField = findDeclaredField(enumClass, codeFieldName);
            Field nameField = findDeclaredField(enumClass, nameFieldName);
            if (codeField == null || nameField == null || !isTypeCompatible(codeField.getType(), propertyType)) {
                continue;
            }

            codeField.setAccessible(true);
            nameField.setAccessible(true);
            return getMetadata(enumClass, codeField, nameField);
        }
        return null;
    }

    private JsonEnumMetadata getMetadata(Class<? extends Enum<?>> enumClass, Field codeField, Field nameField) {
        String cacheKey = enumClass.getName() + ":" + codeField.getName() + ":" + nameField.getName();
        return metadataCache.computeIfAbsent(cacheKey, key -> {
            Map<Object, Object> mapping = new HashMap<>();
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                try {
                    Object code = codeField.get(constant);
                    Object name = nameField.get(constant);
                    if (code != null && name != null) {
                        mapping.put(code, name);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("Failed to read enum fields", e);
                }
            }
            return new JsonEnumMetadata(mapping, codeField.getName(), nameField.getName());
        });
    }

    private Field findDeclaredField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private boolean isTypeCompatible(Class<?> enumFieldType, Class<?> propertyType) {
        if (enumFieldType.isAssignableFrom(propertyType)) {
            return true;
        }
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
}
