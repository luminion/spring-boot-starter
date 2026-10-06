package io.github.luminion.velo.jackson.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;
import tools.jackson.databind.type.SimpleType;

import java.lang.reflect.Array;

/**
 * 基本类型数字数组复用元素的原生序列化规则，不复制或缓存整个数组。
 */
public class NumericArraySerializer<T> extends ArraySerializerBase<T> {

    private final JavaType contentType;
    private final ValueSerializer<Object> contentSerializer;

    public NumericArraySerializer(Class<T> arrayType, Class<? extends Number> contentType) {
        super(arrayType);
        this.contentType = SimpleType.constructUnsafe(contentType);
        this.contentSerializer = null;
    }

    private NumericArraySerializer(NumericArraySerializer<T> source, BeanProperty property,
                                   Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls,
                                   ValueSerializer<Object> contentSerializer) {
        super(source, property, unwrapSingle, suppressableValue, suppressNulls);
        this.contentType = source.contentType;
        this.contentSerializer = contentSerializer;
    }

    @Override
    protected ArraySerializerBase<T> _withResolved(BeanProperty property, Boolean unwrapSingle,
                                                  Object suppressableValue, boolean suppressNulls) {
        return new NumericArraySerializer<>(this, property, unwrapSingle, suppressableValue,
                suppressNulls, contentSerializer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
        NumericArraySerializer<T> resolved = (NumericArraySerializer<T>) super.createContextual(context, property);
        ValueSerializer<?> annotated = findAnnotatedContentSerializer(context, property);
        ValueSerializer<Object> serializer = annotated == null
                ? context.findContentValueSerializer(contentType, property)
                : (ValueSerializer<Object>) context.handleSecondaryContextualization(annotated, property);
        return new NumericArraySerializer<>(resolved, property, resolved._unwrapSingle, resolved._suppressableValue,
                resolved._suppressNulls, serializer);
    }

    @Override
    public JavaType getContentType() {
        return contentType;
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        return contentSerializer;
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer serializer) {
        return this;
    }

    @Override
    public boolean isEmpty(SerializationContext context, T value) {
        return Array.getLength(value) == 0;
    }

    @Override
    public boolean hasSingleElement(T value) {
        return Array.getLength(value) == 1;
    }

    @Override
    public void serialize(T value, JsonGenerator generator, SerializationContext context) {
        boolean filter = _needToCheckFiltering(context);
        if (hasSingleElement(value) && _shouldUnwrapSingle(context)
                && (!filter || _shouldSerializeElement(context, Array.get(value, 0)))) {
            serializeContents(value, generator, context);
            return;
        }
        generator.writeStartArray(value);
        serializeContents(value, generator, context);
        generator.writeEndArray();
    }

    @Override
    protected void serializeContents(T value, JsonGenerator generator, SerializationContext context) {
        ValueSerializer<Object> serializer = contentSerializer;
        if (serializer == null) {
            serializer = context.findContentValueSerializer(contentType, _property);
        }
        boolean filter = _needToCheckFiltering(context);
        for (int index = 0, length = Array.getLength(value); index < length; index++) {
            Object element = Array.get(value, index);
            if (!filter || _shouldSerializeElement(context, element)) {
                serializer.serialize(element, generator, context);
            }
        }
    }
}
