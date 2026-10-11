package io.github.luminion.velo.condition;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 只向通用 Object 值序列化器退让，局部 DTO / String 序列化器不影响默认配置。
 * 各 Boot 版本的 ConditionalOnMissingBean 按原始类型匹配，因此在此保留泛型信息。
 */
public class OnMissingRedisObjectSerializerCondition extends SpringBootCondition implements ConfigurationCondition {

    private static final ResolvableType SERIALIZER_TYPE =
            ResolvableType.forClassWithGenerics(RedisSerializer.class, Object.class);

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory != null) {
            // 只检查定义，不提前初始化用户 Bean；同样检查父容器中的通用候选。
            String[] names = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    beanFactory, SERIALIZER_TYPE, true, false);
            if (names.length > 0) {
                return ConditionOutcome.noMatch("RedisSerializer<Object> bean already defined");
            }
        }
        return ConditionOutcome.match("No RedisSerializer<Object> bean defined");
    }
}
