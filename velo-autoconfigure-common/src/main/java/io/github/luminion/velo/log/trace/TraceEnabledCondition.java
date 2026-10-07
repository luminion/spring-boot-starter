package io.github.luminion.velo.log.trace;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 独立 trace 开关；新配置优先，旧日志下的 trace 配置作为兼容回退。
 */
public class TraceEnabledCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return new ConditionOutcome(isEnabled(context.getEnvironment()), "trace enabled configuration");
    }

    public static boolean isEnabled(Environment environment) {
        return property(environment, "enabled", Boolean.class, true);
    }

    public static <T> T property(Environment environment, String name, Class<T> type, T defaultValue) {
        Binder binder = Binder.get(environment);
        T value = binder.bind("velo.trace." + name, Bindable.of(type)).orElse(null);
        return value != null
                ? value
                : binder.bind("velo.log.trace." + name, Bindable.of(type)).orElse(defaultValue);
    }
}
