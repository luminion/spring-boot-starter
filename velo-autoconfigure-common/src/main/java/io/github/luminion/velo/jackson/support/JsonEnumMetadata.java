package io.github.luminion.velo.jackson.support;

import lombok.Getter;

import java.util.Map;

/**
 * Runtime mapping from enum code values to enum display names.
 */
public class JsonEnumMetadata {

    private final Map<Object, Object> mapping;
    @Getter
    private final String codeFieldName;
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
