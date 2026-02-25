package io.github.luminion.starter;

import io.github.luminion.starter.core.CorePreConfig;
import io.github.luminion.starter.feature.cache.CacheConfig;
import io.github.luminion.starter.feature.converter.ConverterConfig;
import io.github.luminion.starter.feature.idempotent.IdempotentConfig;
import io.github.luminion.starter.feature.jackson.JacksonConfig;
import io.github.luminion.starter.feature.lock.LockConfig;
import io.github.luminion.starter.feature.log.LogConfig;
import io.github.luminion.starter.feature.mybatisplus.MybatisPlusConfig;
import io.github.luminion.starter.feature.ratelimit.RateLimitConfig;
import io.github.luminion.starter.feature.redis.RedisConfig;
import io.github.luminion.starter.feature.web.WebMvcConfig;
import io.github.luminion.starter.feature.xss.XssConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 *
 * Spring Boot 提供了 ConditionEvaluationReport。
 * 用户只需要在 application.yml 里设置 debug: true，或者启动时加 --debug 参数，就能清楚地看到哪些 Bean
 * 因为什么条件加载了，哪些没加载
 *
 * @author luminion
 * @since 1.0.0
 */
@EnableConfigurationProperties(Prop.class)
@Import({
                CacheConfig.class,
                ConverterConfig.class,
                CorePreConfig.class,
                IdempotentConfig.class,
                JacksonConfig.class,
                LockConfig.class,
                LogConfig.class,
                MybatisPlusConfig.class,
                RateLimitConfig.class,
                RedisConfig.class,
                WebMvcConfig.class,
                XssConfig.class
})
public class LuminionAutoConfiguration {
}
