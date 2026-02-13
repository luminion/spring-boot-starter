package io.github.luminion.starter;

import io.github.luminion.starter.autoconfig.*;
import io.github.luminion.starter.autoconfig.CacheConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 *
 * Spring Boot 提供了 ConditionEvaluationReport。
 * 用户只需要在 application.yml 里设置 debug: true，或者启动时加 --debug 参数，就能清楚地看到哪些 Bean 因为什么条件加载了，哪些没加载
 *
 * @author luminion
 * @since 1.0.0
 */
@EnableConfigurationProperties(Prop.class)
@Import({
        CacheConfiguration.class,
//        ConverterConfiguration.class,
        CoreConfiguration.class,
        IdempotentConfiguration.class,
        JacksonConfiguration.class,
        LogConfiguration.class,
        MaskConfiguration.class,
        MybatisPlusConfiguration.class,
        RateLimitConfiguration.class,
        RedisConfiguration.class,
        WebMvcConfiguration.class,
        XssConfiguration.class
})
public class LuminionAutoConfiguration {
}
