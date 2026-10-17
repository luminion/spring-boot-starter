package io.github.luminion.velo.log.core;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogInvocation;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 默认组合与显式完整声明按功能覆盖；配置只控制入口和默认组合选择。
 */
final class InvocationLogPolicyResolver {
    private final VeloProperties properties;

    InvocationLogPolicyResolver(VeloProperties properties) {
        this.properties = properties;
        validate(properties.getLog().getSources().getController());
        validate(properties.getLog().getSources().getFeign());
        validate(properties.getLog().getSources().getScheduled());
        validate(properties.getLog().getSources().getXxlJob());
    }

    private void validate(VeloProperties.InvocationAutoSourceProperties source) {
        Class<? extends Annotation> type = source.getDefaultAnnotation();
        if (type != null) {
            InvocationLogMetadata.preset(type);
        }
    }

    Selection resolve(LogInvocation invocation) {
        InvocationLogMetadata.Metadata metadata =
                InvocationLogMetadata.get(invocation.getMethod(), invocation.getTargetClass());
        VeloProperties.InvocationSourceProperties source = source(invocation.getSource());
        Map<InvocationLogFeature, InvocationLogPolicy> policies = new EnumMap<>(InvocationLogFeature.class);
        if (!metadata.ignored && properties.getLog().isEnabled() && source.isEnabled()) {
            if (source instanceof VeloProperties.InvocationAutoSourceProperties) {
                Class<? extends Annotation> type =
                        ((VeloProperties.InvocationAutoSourceProperties) source).getDefaultAnnotation();
                if (type == null) {
                    boolean httpSource = invocation.getSource() == InvocationLogSource.CONTROLLER
                            || invocation.getSource() == InvocationLogSource.FEIGN;
                    type = httpSource ? InvokeLog.class : TaskDefaults.class;
                }
                policies.putAll(InvocationLogMetadata.preset(type));
            }
            policies.putAll(metadata.annotations);
        }
        return new Selection(metadata, policies);
    }

    private VeloProperties.InvocationSourceProperties source(InvocationLogSource source) {
        VeloProperties.InvocationSources sources = properties.getLog().getSources();
        switch (source) {
            case CONTROLLER:
                return sources.getController();
            case FEIGN:
                return sources.getFeign();
            case SCHEDULED:
                return sources.getScheduled();
            case XXL_JOB:
                return sources.getXxlJob();
            default:
                return sources.getAnnotation();
        }
    }

    @CostLog
    @SlowLog
    @ErrorLog
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TaskDefaults {
    }

    @RequiredArgsConstructor
    static class Selection {
        final InvocationLogMetadata.Metadata metadata;
        final Map<InvocationLogFeature, InvocationLogPolicy> policies;
    }
}
