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

    /** 数组元素对应的 Jackson 类型。 */
    private final JavaType contentType;

    /** 经上下文解析后的数组元素序列化器。 */
    private final ValueSerializer<Object> contentSerializer;

    /**
     * 创建指定数组类型的数字序列化器。
     *
     * @param arrayType 数组类型
     * @param contentType 数组元素的数字类型
     */
    public NumericArraySerializer(Class<T> arrayType, Class<? extends Number> contentType) {
        super(arrayType);
        this.contentType = SimpleType.constructUnsafe(contentType);
        this.contentSerializer = null;
    }

    /** 复制数组配置并保留已解析的元素序列化器。 */
    private NumericArraySerializer(NumericArraySerializer<T> source, BeanProperty property,
                                   Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls,
                                   ValueSerializer<Object> contentSerializer) {
        super(source, property, unwrapSingle, suppressableValue, suppressNulls);
        this.contentType = source.contentType;
        this.contentSerializer = contentSerializer;
    }

    /** 复制当前实例并应用 Jackson 解析出的属性配置。 */
    @Override
    protected ArraySerializerBase<T> _withResolved(BeanProperty property, Boolean unwrapSingle,
                                                  Object suppressableValue, boolean suppressNulls) {
        return new NumericArraySerializer<>(this, property, unwrapSingle, suppressableValue,
                suppressNulls, contentSerializer);
    }

    /** 为当前属性选择元素序列化器，并保留数组级包含设置。 */
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

    /** 按 Jackson 的单元素展开配置写出数组及其元素。 */
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
