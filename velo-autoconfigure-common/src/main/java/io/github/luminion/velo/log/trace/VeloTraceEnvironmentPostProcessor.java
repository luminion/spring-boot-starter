package io.github.luminion.velo.log.trace;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * 将 traceId 加入 Spring Boot 默认日志级别格式。
 */
public class VeloTraceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "veloTraceLoggingPattern";

    private static final String LOG_LEVEL_PATTERN_PROPERTY = "logging.pattern.level";

    @Getter
    private final int order = Ordered.HIGHEST_PRECEDENCE + 20;

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = TraceEnabledCondition.isEnabled(environment);
        boolean patternEnabled =
                TraceEnabledCondition.property(environment, "logging-pattern-enabled", Boolean.class, true);
        if (!enabled
                || !patternEnabled
                || StringUtils.hasText(environment.getProperty(LOG_LEVEL_PATTERN_PROPERTY))) {
            return;
        }
        // 此处理器只把 traceId 追加到默认 level pattern，不改变已有日期配置。
        String mdcKey = TraceEnabledCondition.property(environment, "mdc-key", String.class, "traceId");
        String levelPattern = "%5p [%X{" + mdcKey + "}]";
        Map<String, Object> props = new HashMap<>();
        props.put(LOG_LEVEL_PATTERN_PROPERTY, levelPattern);
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }
}
