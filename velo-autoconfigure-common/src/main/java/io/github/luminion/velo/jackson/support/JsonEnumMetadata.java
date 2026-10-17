package io.github.luminion.velo.jackson.support;

import java.util.Map;

import lombok.Getter;

/**
 * 枚举编码值到显示名称的运行时映射。
 */
public class JsonEnumMetadata {

    /** 枚举编码值到显示名称的映射。 */
    private final Map<Object, Object> mapping;

    /** 编码字段的名称。 */
    @Getter
    private final String codeFieldName;

    /** 显示名称字段的名称。 */
    @Getter
    private final String nameFieldName;

    public JsonEnumMetadata(Map<Object, Object> mapping, String codeFieldName, String nameFieldName) {
        this.mapping = mapping;
        this.codeFieldName = codeFieldName;
        this.nameFieldName = nameFieldName;
    }

    public Object getName(Object code) {
        return mapping.get(code);
    }
}
