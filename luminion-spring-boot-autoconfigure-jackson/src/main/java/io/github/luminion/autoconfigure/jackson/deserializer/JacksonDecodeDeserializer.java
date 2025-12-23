package io.github.luminion.autoconfigure.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.luminion.autoconfigure.jackson.annotation.JacksonDecode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.GenericTypeResolver;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;


/**
 * 杰克逊解码解串器
 * <p>
 * luminion
 * 1.0.0
 */
@Slf4j
public class JacksonDecodeDeserializer<T> extends StdDeserializer<T> implements ContextualDeserializer {

    private final Function<String, T> func;

    // 默认构造
    public JacksonDecodeDeserializer() {
        super(Object.class);
        this.func = null; // 标记为空，表示未初始化
    }

    // 私有构造，用于创建特定配置的实例
    private JacksonDecodeDeserializer(Function<String, T> func) {
        super(Object.class);
        this.func = func;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        // 这一步理论上不会在 func 为 null 时触发，
        // 因为如果 func 为 null，createContextual 会返回默认反序列化器，而不进入这个方法。
        // 但为了代码健壮性，做一个保底：
        if (func == null) {
            // 极低概率进入：如果这里被触发，说明 Contextual 流程没走通
            // 尝试读取文本直接强转（不推荐，但作为最后防线）
            return (T) p.getText();
        }

        String text = p.getText();
        if (text == null) {
            return null;
        }
        return func.apply(text);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        // 1. 既然你的 Deserializer 是绑定在 @JacksonDecode 上的
        // 如果 property 为 null，说明没地方挂注解，直接跳过或者报错（视你的容忍度而定）
        if (property == null) {
            // 这里返回 null 或者默认 deserializer 都可以，通常不会进入这里
            return ctxt.findContextualValueDeserializer(ctxt.getContextualType(), null);
        }

        JacksonDecode annotation = property.getAnnotation(JacksonDecode.class);

        // 2. 只有一种情况需要“兼容”：
        // 那就是你把 JacksonDecodeDeserializer 注册到了全局 ObjectMapper 里，
        // 导致没有注解的字段也跑进来了。
        // 如果你是通过 @JsonDeserialize(using=...) 用的，这里 annotation 绝不会为 null。
        if (annotation == null) {
            // 这是一个普通字段，不归我管，请 Jackson 继续找别人
            // 此时传入 property，完美支持 @JsonFormat
            return ctxt.findContextualValueDeserializer(property.getType(), property);
        }

        // --- 下面全是“强硬”的逻辑，不行就报错 ---

        Class<? extends Function<String, ?>> functionClass = annotation.value();

        // 3. 泛型检查：不通过直接抛异常
        Class<?>[] args = GenericTypeResolver.resolveTypeArguments(functionClass, Function.class);
        if (args == null || args.length != 2) {
            throw JsonMappingException.from(ctxt,
                    String.format("类 %s 必须实现 Function<String, ?> 接口", functionClass.getName()));
        }

        Class<?> argClass = args[0];
        Class<?> returnClass = args[1];
        Class<?> propertyType = property.getType().getRawClass();

        // 4. 类型匹配检查：不通过直接抛异常
        if (!String.class.equals(argClass)) {
            throw JsonMappingException.from(ctxt,
                    String.format("@JacksonDecode 函数入参必须是 String。类: %s", functionClass.getName()));
        }
        if (!propertyType.isAssignableFrom(returnClass)) {
            throw JsonMappingException.from(ctxt,
                    String.format("字段 %s 类型是 %s，但函数返回类型是 %s，无法赋值",
                            property.getName(), propertyType.getName(), returnClass.getName()));
        }

        // 5. 复用优化
        if (this.func != null && Objects.equals(functionClass, this.func.getClass())) {
            return this;
        }

        // 6. 实例化：出错直接抛异常
        try {
            Function<String, ?> instance = functionClass.getDeclaredConstructor().newInstance();
            return new JacksonDecodeDeserializer<>((Function<String, T>) instance);
        } catch (Exception e) {
            throw JsonMappingException.from(ctxt,
                    "无法实例化 @JacksonDecode 指定的函数类: " + functionClass.getName(), e);
        }
    }
}
