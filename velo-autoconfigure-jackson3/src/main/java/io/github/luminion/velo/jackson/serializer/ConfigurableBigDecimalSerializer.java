package io.github.luminion.velo.jackson.serializer;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.jdk.NumberSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.ser.std.ToStringSerializerBase;

import java.math.BigDecimal;

/** 按配置将 BigDecimal 输出为字符串或数字，并可去除尾随零。 */
public class ConfigurableBigDecimalSerializer extends StdSerializer<BigDecimal> {

    /** 是否将值序列化为字符串。 */
    private final boolean asString;

    /** 是否在输出前去除尾随零。 */
    private final boolean stripTrailingZeros;

    /** 根据字符串输出配置选择的 Jackson 原生委托序列化器。 */
    private final ValueSerializer<? super BigDecimal> delegate;

    /**
     * 创建可配置的 BigDecimal 序列化器。
     *
     * @param asString 是否以字符串形式输出
     * @param stripTrailingZeros 是否去除尾随零
     */
    public ConfigurableBigDecimalSerializer(boolean asString, boolean stripTrailingZeros) {
        super(BigDecimal.class);
        this.asString = asString;
        this.stripTrailingZeros = stripTrailingZeros;
        this.delegate = asString ? new PlainBigDecimalSerializer() : new NumberSerializer(BigDecimal.class);
    }

    /** 允许属性级 {@code @JsonFormat} 覆盖全局字符串输出设置。 */
    @Override
    public ValueSerializer<?> createContextual(SerializationContext context, BeanProperty property) {
        JsonFormat.Value format = findFormatOverrides(context, property, BigDecimal.class);
        boolean resolved = asString;
        if (format != null && format.getShape() != JsonFormat.Shape.ANY) {
            resolved = format.getShape() == JsonFormat.Shape.STRING;
        }
        return resolved == asString ? this : new ConfigurableBigDecimalSerializer(resolved, stripTrailingZeros);
    }

    /** 按配置规范化 BigDecimal，再交由原生序列化器输出。 */
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt) {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serialize(normalized, gen, ctxt);
    }

    /** 保留原生多态类型处理，并按配置规范化 BigDecimal。 */
    @Override
    public void serializeWithType(BigDecimal value, JsonGenerator generator, SerializationContext context,
                                  TypeSerializer typeSerializer) {
        BigDecimal normalized = stripTrailingZeros ? value.stripTrailingZeros() : value;
        delegate.serializeWithType(normalized, generator, context, typeSerializer);
    }

    /** 使用普通十进制表示法输出 BigDecimal 字符串。 */
    private static class PlainBigDecimalSerializer extends ToStringSerializerBase {
        PlainBigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public String valueToString(Object value) {
            return ((BigDecimal) value).toPlainString();
        }
    }
}
