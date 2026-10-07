package io.github.luminion.velo.log.condition;

import io.github.luminion.velo.trace.TraceEnabledCondition;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 入口适配只负责组合独立的日志和 trace 功能。
 */
class InvocationAdapterCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnInvocationAdapter.class.getName());
        String source = (String) attributes.get("source");
        Environment environment = context.getEnvironment();
        Boolean enabled = environment.getProperty("velo.log.enabled", Boolean.class, true);
        Boolean sourcesEnabled = environment.getProperty("velo.log.sources." + source + ".enabled", Boolean.class, true);
        boolean logEnabled = enabled && sourcesEnabled;
        return new ConditionOutcome(logEnabled || TraceEnabledCondition.isEnabled(environment), "invocation logging or trace enabled for " + source);
    }
}
