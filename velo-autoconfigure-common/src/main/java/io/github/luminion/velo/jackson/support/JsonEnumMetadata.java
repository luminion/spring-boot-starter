package io.github.luminion.velo.jackson.support;

import java.util.Map;

/**
 * Runtime mapping from enum code values to enum display names.
 */
public class JsonEnumMetadata {

    private final Map<Object, Object> mapping;
    private final String codeFieldName;
    private final String nameFieldName;

    public JsonEnumMetadata(Map<Object, Object> mapping, String codeFieldName, String nameFieldName) {
        this.mapping = mapping;
        this.codeFieldName = codeFieldName;
        this.nameFieldName = nameFieldName;
    }

    public Object getName(Object code) {
        return mapping.get(code);
    }

    public String getCodeFieldName() {
        return codeFieldName;
    }

    public String getNameFieldName() {
        return nameFieldName;
    }
}
