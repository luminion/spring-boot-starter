package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.std.ArraySerializerBase;
import com.fasterxml.jackson.databind.type.SimpleType;

import java.io.IOException;
import java.lang.reflect.Array;

/**
 * 基本类型数字数组复用元素的原生序列化规则，不复制或缓存整个数组。
 */
public class NumericArraySerializer<T> extends ArraySerializerBase<T> {

    private final JavaType contentType;
    private final JsonSerializer<Object> contentSerializer;

    public NumericArraySerializer(Class<T> arrayType, Class<? extends Number> contentType) {
        super(arrayType);
        this.contentType = SimpleType.constructUnsafe(contentType);
        this.contentSerializer = null;
    }

    private NumericArraySerializer(NumericArraySerializer<T> source, BeanProperty property,
                                   Boolean unwrapSingle, JsonSerializer<Object> contentSerializer) {
        super(source, property, unwrapSingle);
        this.contentType = source.contentType;
        this.contentSerializer = contentSerializer;
    }

    @Override
    public JsonSerializer<?> _withResolved(BeanProperty property, Boolean unwrapSingle) {
        return new NumericArraySerializer<>(this, property, unwrapSingle, contentSerializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
            throws JsonMappingException {
        NumericArraySerializer<T> resolved = (NumericArraySerializer<T>) super.createContextual(provider, property);
        JsonSerializer<?> annotated = findAnnotatedContentSerializer(provider, property);
        JsonSerializer<Object> serializer = annotated == null
                ? provider.findValueSerializer(contentType, property)
                : (JsonSerializer<Object>) provider.handleSecondaryContextualization(annotated, property);
        return new NumericArraySerializer<>(resolved, property, resolved._unwrapSingle, serializer);
    }

    @Override
    public JavaType getContentType() {
        return contentType;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return contentSerializer;
    }

    @Override
    protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer serializer) {
        return this;
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, T value) {
        return Array.getLength(value) == 0;
    }

    @Override
    public boolean hasSingleElement(T value) {
        return Array.getLength(value) == 1;
    }

    @Override
    protected void serializeContents(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        JsonSerializer<Object> serializer = contentSerializer;
        if (serializer == null) {
            serializer = provider.findValueSerializer(contentType, _property);
        }
        for (int index = 0, length = Array.getLength(value); index < length; index++) {
            serializer.serialize(Array.get(value, index), generator, provider);
        }
    }
}
