package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.luminion.velo.condition.OnMissingRedisObjectSerializerCondition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.core.KotlinDetector;
import org.springframework.util.ClassUtils;

/**
 * Jackson 2 的 Redis JSON 适配，与 HTTP Jackson 增强开关和 mapper 隔离。
 */
@AutoConfiguration(beforeName = {
        "io.github.luminion.velo.redis.VeloRedisAutoConfiguration",
        "io.github.luminion.velo.cache.VeloCacheAutoConfiguration"
})
@ConditionalOnClass({ObjectMapper.class, RedisSerializer.class})
public class VeloRedisJsonAutoConfiguration {

    @Bean
    @Conditional(OnMissingRedisObjectSerializerCondition.class)
    public RedisSerializer<Object> veloRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new JavaTimeModule());
        // 默认信任 Redis 及其内容；通用缓存有意按类型元数据回读 POJO，不额外限定业务类型白名单。
        // 需要限制类型时由业务提供定向 serializer 或自定义 RedisSerializer<Object> Bean。
        mapper.setDefaultTyping(new RedisTypeResolverBuilder()
                .init(JsonTypeInfo.Id.CLASS, null)
                .inclusion(JsonTypeInfo.As.PROPERTY));
        GenericJackson2JsonRedisSerializer.registerNullValueSerializer(mapper, null);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * 沿用 Spring Data Generic JSON 的类型判定，兼容共享模块支持的 Redis 2.x/3.x。
     * 普通 NON_FINAL 策略会漏掉 final DTO 和 record；Java 标量与日期仍由各自序列化器处理。
     */
    private static final class RedisTypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {
        private static final long serialVersionUID = 1L;

        private RedisTypeResolverBuilder() {
            super(ObjectMapper.DefaultTyping.NON_FINAL, LaissezFaireSubTypeValidator.instance);
        }

        @Override
        public ObjectMapper.DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
            return this;
        }

        @Override
        public boolean useForType(JavaType type) {
            if (type.isJavaLangObject()) {
                return true;
            }
            while (type.isArrayType() || type.isReferenceType()) {
                if (type.isArrayType()) {
                    type = type.getContentType();
                } else {
                    type = type.getReferencedType();
                }
            }
            Class<?> rawClass = type.getRawClass();
            if (type.isEnumType() || ClassUtils.isPrimitiveOrWrapper(rawClass)) {
                return false;
            }
            if (type.isFinal() && !KotlinDetector.isKotlinType(rawClass)
                    && rawClass.getPackage().getName().startsWith("java")) {
                return false;
            }
            return !TreeNode.class.isAssignableFrom(rawClass);
        }
    }
}
