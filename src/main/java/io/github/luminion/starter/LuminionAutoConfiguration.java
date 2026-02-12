package io.github.luminion.starter;

import io.github.luminion.starter.autoconfig.*;
import io.github.luminion.starter.autoconfig.CacheConfiguration;
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
        ConverterConfiguration.class,
        MybatisPlusConfiguration.class,
        RedisConfiguration.class,
        CacheConfiguration.class,
        LogConfiguration.class
})
public class LuminionAutoConfiguration {
}
