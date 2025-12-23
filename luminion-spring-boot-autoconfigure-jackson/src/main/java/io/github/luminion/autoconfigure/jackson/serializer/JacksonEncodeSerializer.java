package io.github.luminion.autoconfigure.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.luminion.autoconfigure.jackson.annotation.JsonEncode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.GenericTypeResolver;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

/**
 * 杰克逊编码序列化器
 * <p>
 * 对应 JsonEncode 注解，处理对象到字符串的自定义转换
 *
 * @author luminion
 */
@Slf4j
public class JacksonEncodeSerializer extends StdSerializer<Object> implements ContextualSerializer {

    // 内部持有的转换函数
    // 泛型定义为 Object -> String，因为在运行时 serialize 方法只知道 value 是 Object
    private final Function<Object, String> func;

    /**
     * 默认无参构造
     * 用于 Jackson 首次加载该 Serializer 类作为“工厂”使用
     */
    public JacksonEncodeSerializer() {
        super(Object.class);
        this.func = null; // 标记为空，表示未初始化
    }

    /**
     * 私有构造
     * 用于 createContextual 创建真正干活的 Worker 实例
     */
    private JacksonEncodeSerializer(Function<Object, String> func) {
        super(Object.class);
        this.func = func;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // 1. 容错逻辑：理论上 Contextual 流程走通后 func 不会为 null
        // 如果为 null，说明可能被错误地直接使用了，兜底调用 toString
        if (func == null) {
            if (value == null) {
                gen.writeNull();
            } else {
                gen.writeString(value.toString());
            }
            return;
        }

        // 2. 处理 null 值
        if (value == null) {
            gen.writeNull();
            return;
        }

        // 3. 执行转换
        // 这里不需要 try-catch，让函数的异常直接抛出，Jackson 会包装它
        String result = func.apply(value);

        // 4. 写入结果
        if (result == null) {
            gen.writeNull();
        } else {
            gen.writeString(result);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        // 1. 如果 property 为 null（例如根对象序列化），无法获取注解
        // 直接返回原生的序列化器（通常是 BeanSerializer）
        if (property == null) {
            return this;
        }

        JsonEncode annotation = property.getAnnotation(JsonEncode.class);

        // 2. 如果没有注解，说明不是我们要处理的字段
        // 交还给 Jackson，让它去查找该类型标准的序列化器
        if (annotation == null) {
            return prov.findValueSerializer(property.getType(), property);
        }

        // --- 进入强硬的校验逻辑 ---

        Class<? extends Function<?, String>> functionClass = annotation.value();

        // 3. 泛型检查
        // 解析 Function<IN, OUT> 的具体的类型
        Class<?>[] args = GenericTypeResolver.resolveTypeArguments(functionClass, Function.class);
        if (args == null || args.length != 2) {
            throw JsonMappingException.from(prov,
                    String.format("Class %s must implement the Function<?, String> interface", functionClass.getName()));
        }

        Class<?> argInputClass = args[0];  // 函数的入参类型 (例如 User)
        Class<?> argReturnClass = args[1]; // 函数的返参类型 (必须是 String)
        Class<?> propertyType = property.getType().getRawClass(); // 字段的实际类型

        // 4. 类型匹配检查

        // 检查 A: 函数返回类型必须是 String
        if (!String.class.equals(argReturnClass)) {
            throw JsonMappingException.from(prov,
                    String.format("@JsonEncode function return type must be String. Class: %s", functionClass.getName()));
        }

        // 检查 B: 字段类型必须能传给函数入参
        // 例如：字段是 User 类型，函数是 Function<User, String> -> OK
        //      字段是 User 类型，函数是 Function<Object, String> -> OK (User 是 Object 子类)
        //      字段是 Object 类型，函数是 Function<User, String> -> ERROR (父类不能传给子类参数)
        if (!argInputClass.isAssignableFrom(propertyType)) {
            throw JsonMappingException.from(prov,
                    String.format("Field %s is of type %s, but function %s requires an input parameter of type %s, which is not compatible",
                            property.getName(),
                            propertyType.getName(),
                            functionClass.getName(),
                            argInputClass.getName()));
        }

        // 5. 复用优化 (如果当前实例已经持有该类型的函数，直接返回自己)
        // 注意：这里仅判断类是否相同，如果你的 Function 是有状态的（带参数构造），不能这样复用
        if (this.func != null && Objects.equals(functionClass, this.func.getClass())) {
            return this;
        }

        // 6. 实例化
        try {
            Function<?, String> instance = functionClass.getDeclaredConstructor().newInstance();
            // 强转为 Object -> String，这是安全的，因为上面的 isAssignableFrom 已经保证了类型兼容
            return new JacksonEncodeSerializer((Function<Object, String>) instance);
        } catch (Exception e) {
            throw JsonMappingException.from(prov,
                    "Cannot instantiate the function class specified by @JsonEncode: " + functionClass.getName(), e);
        }
    }
}
