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

    /** 数组元素的 Jackson 类型信息。 */
    private final JavaType contentType;

    /** 数组元素对应的序列化器；为空时由提供器按需解析。 */
    private final JsonSerializer<Object> contentSerializer;

    /** 创建使用指定数组类型和元素类型的序列化器。 */
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

    /** 基于当前属性配置复制数组序列化器。 */
    @Override
    public JsonSerializer<?> _withResolved(BeanProperty property, Boolean unwrapSingle) {
        return new NumericArraySerializer<>(this, property, unwrapSingle, contentSerializer);
    }

    /** 解析当前属性的元素序列化器。 */
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

    /** 返回数组元素的 Jackson 类型。 */
    @Override
    public JavaType getContentType() {
        return contentType;
    }

    /** 返回已解析的数组元素序列化器。 */
    @Override
    public JsonSerializer<?> getContentSerializer() {
        return contentSerializer;
    }

    @Override
    protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer serializer) {
        return this;
    }

    /** 判断数字数组是否没有元素。 */
    @Override
    public boolean isEmpty(SerializerProvider provider, T value) {
        return Array.getLength(value) == 0;
    }

    /** 判断数组是否只包含一个元素。 */
    @Override
    public boolean hasSingleElement(T value) {
        return Array.getLength(value) == 1;
    }

    /** 使用元素序列化器逐项写出数组内容。 */
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
