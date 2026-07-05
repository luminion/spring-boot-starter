package io.github.luminion.velo.log.trace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds trace id to Spring Boot default log level pattern.
 */
public class VeloTraceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "veloTraceLoggingPattern";

    private static final String LOG_LEVEL_PATTERN_PROPERTY = "logging.pattern.level";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean logEnabled = environment.getProperty("velo.log.enabled", Boolean.class, true);
        boolean enabled = environment.getProperty("velo.log.trace.enabled", Boolean.class, true);
        boolean patternEnabled = environment.getProperty("velo.log.trace.logging-pattern-enabled", Boolean.class, true);
        if (!logEnabled || !enabled || !patternEnabled
                || StringUtils.hasText(environment.getProperty(LOG_LEVEL_PATTERN_PROPERTY))) {
            return;
        }
        // 只把 traceId 追加到默认 level pattern，作为增强；日期格式保持 Spring Boot 默认，不改变全局行为。
        String mdcKey = environment.getProperty("velo.log.trace.mdc-key", "traceId");
        String levelPattern = "%5p [%X{" + mdcKey + "}]";
        Map<String, Object> props = new HashMap<>();
        props.put(LOG_LEVEL_PATTERN_PROPERTY, levelPattern);
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
