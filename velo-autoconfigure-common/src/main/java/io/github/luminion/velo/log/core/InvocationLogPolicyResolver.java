package io.github.luminion.velo.log.core;


import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogMetadata.FeatureOverride;
import io.github.luminion.velo.log.core.InvocationLogMetadata.Metadata;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;

/**
 * 按目标类缓存静态方法元数据；每次调用重新读取来源和全局配置。
 */
final class InvocationLogPolicyResolver {
    private final VeloProperties properties;

    InvocationLogPolicyResolver(VeloProperties properties) {
        this.properties = properties;
    }

    Selection resolve(LogInvocation invocation) {
        Metadata metadata = InvocationLogMetadata.get(invocation.getMethod(), invocation.getTargetClass());
        VeloProperties.InvocationSourceProperties source = source(invocation.getSource());
        boolean ignored = metadata.ignored || !properties.getLog().isEnabled() || !source.isEnabled();
        Map<InvocationLogFeature, FeaturePolicy> policies = new EnumMap<>(InvocationLogFeature.class);
        if (!ignored) {
            for (InvocationLogFeature feature : InvocationLogFeature.values()) {
                FeatureOverride annotation = metadata.annotations.get(feature);
                InvocationLogSource baseline = metadata.profile ? InvocationLogSource.CONTROLLER : invocation.getSource();
                FeatureSettings settings = configured(baseline, feature, source);
                boolean selected = invocation.getSource() != InvocationLogSource.ANNOTATION
                        || metadata.profile || annotation != null;
                if (annotation != null) {
                    settings.enabled = annotation.enabled;
                    if (annotation.level != null) {
                        settings.level = annotation.level;
                    }
                    if (annotation.threshold != null) {
                        settings.threshold = annotation.threshold;
                    }
                    if (annotation.allowlist != null) {
                        settings.allowlist = annotation.allowlist;
                    }
                }
                policies.put(feature, new FeaturePolicy(selected && settings.enabled,
                        settings.level, settings.threshold, settings.allowlist));
            }
        }
        return new Selection(
                metadata, policies, properties.getLog().getDefaults().getMaxPayloadLength() != 0);
    }

    private FeatureSettings configured(
            InvocationLogSource origin,
            InvocationLogFeature feature,
            VeloProperties.InvocationSourceProperties source) {
        VeloProperties.InvocationDefaults defaults = properties.getLog().getDefaults();
        VeloProperties.LogFeatureProperties local = feature(source, feature);
        VeloProperties.LogFeatureProperties global = feature(defaults, feature);
        boolean enabled = builtinEnabled(origin, feature);
        LogLevel level =
                feature == InvocationLogFeature.SLOW_LOG || feature == InvocationLogFeature.ERROR_LOG
                        ? LogLevel.WARN
                        : LogLevel.INFO;
        if (global.getEnabled() != null) {
            enabled = global.getEnabled();
        }
        if (global.getLevel() != null) {
            level = global.getLevel();
        }
        if (local.getEnabled() != null) {
            enabled = local.getEnabled();
        }
        if (local.getLevel() != null) {
            level = local.getLevel();
        }
        long threshold = 1000;
        List<String> allowlist = Collections.emptyList();
        if (feature == InvocationLogFeature.SLOW_LOG) {
            Long inherited = ((VeloProperties.SlowLogProperties) global).getThreshold();
            Long explicit = ((VeloProperties.SlowLogProperties) local).getThreshold();
            if (inherited != null) {
                threshold = inherited;
            }
            if (explicit != null) {
                threshold = explicit;
            }
        } else if (feature == InvocationLogFeature.REQUEST_HEADERS
                || feature == InvocationLogFeature.RESPONSE_HEADERS) {
            List<String> inherited = ((VeloProperties.HeaderCaptureProperties) global).getAllowlist();
            List<String> explicit = ((VeloProperties.HeaderCaptureProperties) local).getAllowlist();
            if (inherited != null) {
                allowlist = inherited;
            }
            if (explicit != null) {
                allowlist = explicit;
            }
        }
        return new FeatureSettings(enabled, level, threshold, allowlist);
    }

    private boolean builtinEnabled(InvocationLogSource source, InvocationLogFeature feature) {
        switch (feature) {
            case ENTRY_ARGS:
            case EXIT_RESULT:
                return source == InvocationLogSource.CONTROLLER
                        || source == InvocationLogSource.FEIGN
                        || source == InvocationLogSource.ANNOTATION;
            case COST_LOG:
            case SLOW_LOG:
            case ERROR_LOG:
                return true;
            default:
                return false;
        }
    }

    private VeloProperties.LogFeatureProperties feature(
            VeloProperties.InvocationDefaults p, InvocationLogFeature f) {
        switch (f) {
            case ENTRY_ARGS:
                return p.getEntryArgs();
            case EXIT_ARGS:
                return p.getExitArgs();
            case EXIT_RESULT:
                return p.getExitResult();
            case COST_LOG:
                return p.getCostLog();
            case SLOW_LOG:
                return p.getSlowLog();
            case REQUEST_HEADERS:
                return p.getRequestHeaders();
            case RESPONSE_HEADERS:
                return p.getResponseHeaders();
            default:
                return p.getErrorLog();
        }
    }

    private VeloProperties.LogFeatureProperties feature(
            VeloProperties.InvocationSourceProperties p, InvocationLogFeature f) {
        switch (f) {
            case ENTRY_ARGS:
                return p.getEntryArgs();
            case EXIT_ARGS:
                return p.getExitArgs();
            case EXIT_RESULT:
                return p.getExitResult();
            case COST_LOG:
                return p.getCostLog();
            case SLOW_LOG:
                return p.getSlowLog();
            case REQUEST_HEADERS:
                return p.getRequestHeaders();
            case RESPONSE_HEADERS:
                return p.getResponseHeaders();
            default:
                return p.getErrorLog();
        }
    }

    private VeloProperties.InvocationSourceProperties source(InvocationLogSource source) {
        VeloProperties.InvocationSources p = properties.getLog().getSources();
        switch (source) {
            case CONTROLLER:
                return p.getController();
            case FEIGN:
                return p.getFeign();
            case SCHEDULED:
                return p.getScheduled();
            case XXL_JOB:
                return p.getXxlJob();
            default:
                return p.getAnnotation();
        }
    }

    private static final class FeatureSettings {
        boolean enabled;
        LogLevel level;
        long threshold;
        List<String> allowlist;

        FeatureSettings(boolean enabled, LogLevel level, long threshold, List<String> allowlist) {
            this.enabled = enabled;
            this.level = level;
            this.threshold = threshold;
            this.allowlist = allowlist;
        }
    }

    @RequiredArgsConstructor
    static class Selection {
        final Metadata metadata;
        final Map<InvocationLogFeature, FeaturePolicy> policies;
        final boolean payloadEnabled;
    }

    static class FeaturePolicy {
        final boolean enabled;
        final LogLevel level;
        final long threshold;
        final List<String> allowlist;

        FeaturePolicy(boolean enabled, LogLevel level, long threshold, List<String> allowlist) {
            this.enabled = enabled && level != LogLevel.OFF;
            this.level = level;
            this.threshold = threshold;
            this.allowlist = Collections.unmodifiableList(new java.util.ArrayList<>(allowlist));
        }
    }
}
