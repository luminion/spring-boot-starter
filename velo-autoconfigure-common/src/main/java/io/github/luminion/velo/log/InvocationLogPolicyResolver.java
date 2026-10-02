package io.github.luminion.velo.log;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.SlowLog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 仅缓存静态方法元数据；每次调用重新读取来源和全局配置。
 */
final class InvocationLogPolicyResolver {
    private final VeloProperties properties;
    private final ConcurrentMap<MetadataKey, Metadata> cache = new ConcurrentHashMap<>();
    private final DefaultParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();

    InvocationLogPolicyResolver(VeloProperties properties) {
        this.properties = properties;
    }

    Selection resolve(LogInvocation invocation) {
        MetadataKey key = new MetadataKey(invocation.getMethod(), invocation.getTargetClass());
        Metadata metadata = cache.computeIfAbsent(key, this::inspect);
        VeloProperties.InvocationSourceProperties source = source(invocation.getSource());
        boolean ignored = metadata.ignored || !properties.getLog().isEnabled() || !source.isEnabled();
        Map<InvocationLogFeature, FeaturePolicy> policies = new EnumMap<>(InvocationLogFeature.class);
        if (!ignored) {
            for (InvocationLogFeature feature : InvocationLogFeature.values()) {
                FeaturePolicy annotation = metadata.annotations.get(feature);
                policies.put(
                        feature,
                        annotation == null ? configured(invocation.getSource(), feature, source) : annotation);
            }
        }
        return new Selection(
                metadata, policies, properties.getLog().getDefaults().getMaxPayloadLength());
    }

    private Metadata inspect(MetadataKey key) {
        Method original = key.method;
        Method specific = AopUtils.getMostSpecificMethod(original, key.type);
        boolean ignored = find(specific, original, key.type, LogIgnore.class) != null;
        Map<InvocationLogFeature, FeaturePolicy> annotations =
                new EnumMap<>(InvocationLogFeature.class);
        for (InvocationLogFeature feature : InvocationLogFeature.values()) {
            Annotation value = find(specific, original, key.type, annotationType(feature));
            if (value != null) {
                annotations.put(feature, fromAnnotation(value));
            }
        }
        String[] parameterNames = names.getParameterNames(specific);
        if (parameterNames == null) {
            parameterNames = names.getParameterNames(original);
        }
        if (parameterNames == null) {
            parameterNames = new String[original.getParameterCount()];
            for (int i = 0; i < parameterNames.length; i++) {
                parameterNames[i] = "arg" + i;
            }
        }
        return new Metadata(
                ignored, specific.getReturnType() == Void.TYPE, parameterNames, annotations);
    }

    private <A extends Annotation> A find(
            Method specific, Method original, Class<?> type, Class<A> kind) {
        // Ignore 是范围排除，类级忽略不能被方法上的其他配置覆盖。
        A found = AnnotatedElementUtils.findMergedAnnotation(specific, kind);
        if (found == null) {
            found = AnnotatedElementUtils.findMergedAnnotation(original, kind);
        }
        if (found == null) {
            found = AnnotatedElementUtils.findMergedAnnotation(type, kind);
        }
        if (found == null) {
            found = AnnotatedElementUtils.findMergedAnnotation(original.getDeclaringClass(), kind);
        }
        return found;
    }

    private FeaturePolicy fromAnnotation(Annotation value) {
        try {
            Class<? extends Annotation> type = value.annotationType();
            boolean enabled = (Boolean) type.getMethod("enabled").invoke(value);
            LogLevel level = (LogLevel) type.getMethod("level").invoke(value);
            long threshold = value instanceof SlowLog ? ((SlowLog) value).thresholdMs() : 0;
            List<String> allowlist = Collections.emptyList();
            if (value instanceof RequestHeadersLog) {
                allowlist = Arrays.asList(((RequestHeadersLog) value).allowlist());
            } else if (value instanceof ResponseHeadersLog) {
                allowlist = Arrays.asList(((ResponseHeadersLog) value).allowlist());
            }
            return new FeaturePolicy(enabled, level, threshold, allowlist);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect logging annotation", error);
        }
    }

    private FeaturePolicy configured(
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
            Long inherited = ((VeloProperties.SlowLogProperties) global).getThresholdMs();
            Long explicit = ((VeloProperties.SlowLogProperties) local).getThresholdMs();
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
        return new FeaturePolicy(enabled, level, threshold, allowlist);
    }

    private boolean builtinEnabled(InvocationLogSource source, InvocationLogFeature feature) {
        switch (feature) {
            case ENTRY_ARGS:
                return source == InvocationLogSource.CONTROLLER
                        || source == InvocationLogSource.FEIGN
                        || source == InvocationLogSource.INVOKE;
            case EXIT_RESULT:
                return source == InvocationLogSource.INVOKE;
            case SLOW_LOG:
            case ERROR_LOG:
                return true;
            default:
                return false;
        }
    }

    private Class<? extends Annotation> annotationType(InvocationLogFeature feature) {
        switch (feature) {
            case ENTRY_ARGS:
                return EntryArgs.class;
            case EXIT_ARGS:
                return ExitArgs.class;
            case EXIT_RESULT:
                return ExitResult.class;
            case SLOW_LOG:
                return SlowLog.class;
            case REQUEST_HEADERS:
                return RequestHeadersLog.class;
            case RESPONSE_HEADERS:
                return ResponseHeadersLog.class;
            default:
                return ErrorLog.class;
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
                return p.getInvoke();
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class MetadataKey {
        private final Method method;
        private final Class<?> type;
    }

    @RequiredArgsConstructor
    static class Metadata {
        final boolean ignored;
        final boolean voidResult;
        final String[] parameterNames;
        final Map<InvocationLogFeature, FeaturePolicy> annotations;
    }

    @RequiredArgsConstructor
    static class Selection {
        final Metadata metadata;
        final Map<InvocationLogFeature, FeaturePolicy> policies;
        final int maxLength;
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
