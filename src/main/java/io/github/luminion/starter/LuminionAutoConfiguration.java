package io.github.luminion.starter;

import io.github.luminion.starter.autoconfig.CacheAutoConfiguration;
import io.github.luminion.starter.autoconfig.LogAutoConfiguration;
import io.github.luminion.starter.autoconfig.MybatisPlusAutoConfiguration;
import io.github.luminion.starter.autoconfig.RedisAutoConfiguration;
import io.github.luminion.starter.autoconfig.ConverterAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * @author luminion
 * @since 1.0.0
 */
@EnableConfigurationProperties(Prop.class)
@Import({
        AopAutoConfiguration.class,
        ConverterAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class,
        RedisAutoConfiguration.class,
        CacheAutoConfiguration.class,
        LogAutoConfiguration.class
})
public class LuminionAutoConfiguration {
}
