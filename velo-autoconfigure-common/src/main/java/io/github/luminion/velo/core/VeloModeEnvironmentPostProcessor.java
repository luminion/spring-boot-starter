package io.github.luminion.velo.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Applies mode-specific starter defaults without overriding user configuration.
 */
public class VeloModeEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "veloModeDefaults";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String mode = environment.getProperty("velo.mode", "OPINIONATED");
        if (!"CONSERVATIVE".equals(mode.toUpperCase(Locale.ENGLISH))) {
            return;
        }
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("velo.log.trace.enabled", "false");
        defaults.put("velo.log.invocation.controller.enabled", "false");
        defaults.put("velo.log.invocation.feign.enabled", "false");
        defaults.put("velo.jackson.enabled", "false");
        defaults.put("velo.spring-converter.date-time-enabled", "false");
        defaults.put("velo.mybatis-plus.enabled", "false");
        defaults.put("velo.cache.enabled", "false");
        defaults.put("velo.redis.enabled", "false");
        defaults.put("velo.excel.converters.enabled", "false");
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
