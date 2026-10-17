package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase;

import java.io.IOException;
import java.math.BigDecimal;

/** 按全局默认值及属性格式配置序列化的 BigDecimal。 */
public class ConfigurableBigDecimalSerializer extends StdSerializer<BigDecimal> implements ContextualSerializer {

    /** 是否将数值写成字符串。 */
    private final boolean asString;

    /** 是否在写出前移除小数末尾的零。 */
    private final boolean stripTrailingZeros;

    /** 根据字符串或数值格式执行实际写出的 Jackson 序列化器。 */
    private final JsonSerializer<? super BigDecimal> delegate;

    /** 创建使用指定默认字符串格式与尾零策略的序列化器。 */
    public ConfigurableBigDecimalSerializer(boolean asString, boolean stripTrailingZeros) {
        super(BigDecimal.class);
        this.asString = asString;
        this.stripTrailingZeros = stripTrailingZeros;
        this.delegate = asString ? new PlainBigDecimalSerializer() : new NumberSerializer(BigDecimal.class);
    }

    /** 解析属性格式覆盖，并返回适用于当前属性的序列化器。 */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property)
            throws JsonMappingException {
        JsonFormat.Value format = findFormatOverrides(provider, property, BigDecimal.class);
        boolean resolved = asString;
        if (format != null && format.getShape() != JsonFormat.Shape.ANY) {
            resolved = format.getShape() == JsonFormat.Shape.STRING;
        }
        return resolved == asString ? this : new ConfigurableBigDecimalSerializer(resolved, stripTrailingZeros);
    }

    /** 根据配置标准化并写出 BigDecimal。 */
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serialize(normalized, gen, serializers);
    }

    /** 在保留 Jackson 类型信息的情况下写出 BigDecimal。 */
    @Override
    public void serializeWithType(BigDecimal value, JsonGenerator generator, SerializerProvider provider,
                                  TypeSerializer typeSerializer) throws IOException {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serializeWithType(normalized, generator, provider, typeSerializer);
    }

    private static class PlainBigDecimalSerializer extends ToStringSerializerBase {
        /** 创建始终使用普通十进制表示的序列化器。 */
        PlainBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        /** 将 BigDecimal 转换为不使用科学计数法的普通十进制字符串。 */
        @Override
        public String valueToString(Object value) {
            return ((BigDecimal) value).toPlainString();
        }
    }
}
