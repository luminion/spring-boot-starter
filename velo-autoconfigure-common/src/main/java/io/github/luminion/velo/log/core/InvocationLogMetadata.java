package io.github.luminion.velo.log.core;

import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.annotation.LogPolicyLevel;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** 入口匹配与策略解析共享的静态元数据；缓存随目标类一起释放。 */
public final class InvocationLogMetadata {
    private static final Map<Class<? extends Annotation>, InvocationLogFeature> FEATURES = features();
    private static final ClassValue<MethodCache> CACHE = new ClassValue<MethodCache>() {
        @Override
        protected MethodCache computeValue(Class<?> type) {
            return new MethodCache();
        }
    };

    private InvocationLogMetadata() {
    }

    /** 判断普通注解入口；自动来源继续由原适配器提供上下文，Ignore 单独不创建入口。 */
    public static boolean isOrdinaryEntry(Method method, Class<?> targetClass) {
        Metadata metadata = get(method, targetClass);
        return !metadata.automaticSource && (metadata.profile || !metadata.annotations.isEmpty());
    }

    static Metadata get(Method method, Class<?> targetClass) {
        Class<?> type = targetClass == null ? method.getDeclaringClass() : targetClass;
        MethodCache cache = CACHE.get(type);
        return cache.methods.computeIfAbsent(method, original -> inspect(original, type, cache.names));
    }

    private static Metadata inspect(Method original, Class<?> type, DefaultParameterNameDiscoverer names) {
        Method specific = AopUtils.getMostSpecificMethod(original, type);
        Map<Object, Map<Class<? extends Annotation>, Annotation>> scopes = new LinkedHashMap<>();
        collect(specific, scopes);
        collect(original, scopes);
        collect(type, scopes);
        collect(original.getDeclaringClass(), scopes);
        boolean profile = false;
        boolean ignored = false;
        Map<InvocationLogFeature, FeatureOverride> overrides = new EnumMap<>(InvocationLogFeature.class);
        List<Map<Class<? extends Annotation>, Annotation>> ordered = new ArrayList<>(scopes.values());
        // 低层级先写入；更细的声明仅覆盖有明确值的参数。
        Collections.reverse(ordered);
        for (Map<Class<? extends Annotation>, Annotation> scope : ordered) {
            profile |= scope.containsKey(InvokeLog.class);
            ignored |= scope.containsKey(LogIgnore.class);
            for (Map.Entry<Class<? extends Annotation>, Annotation> entry : scope.entrySet()) {
                InvocationLogFeature feature = FEATURES.get(entry.getKey());
                if (feature != null) {
                    overrides.put(feature, overlay(overrides.get(feature), entry.getValue()));
                }
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
        boolean automaticSource = automaticSource(specific, original, type);
        return new Metadata(profile, ignored, automaticSource, specific.getReturnType() == Void.TYPE,
                parameterNames, Collections.unmodifiableMap(overrides));
    }

    private static void collect(AnnotatedElement element,
            Map<Object, Map<Class<? extends Annotation>, Annotation>> scopes) {
        MergedAnnotations annotations = MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        for (MergedAnnotation<Annotation> annotation : annotations) {
            Class<? extends Annotation> kind = annotation.getType();
            if (annotation.getDistance() != 0
                    || !(FEATURES.containsKey(kind) || kind == InvokeLog.class || kind == LogIgnore.class)) {
                continue;
            }
            // 不扁平化 InvokeLog 元注解：它只声明套餐，不能盖过配置；业务组合暂不接入。
            Map<Class<? extends Annotation>, Annotation> scope =
                    scopes.computeIfAbsent(annotation.getSource(), ignored -> new LinkedHashMap<>());
            scope.putIfAbsent(kind, annotation.synthesize());
        }
    }

    private static FeatureOverride overlay(FeatureOverride inherited, Annotation value) {
        try {
            Class<? extends Annotation> kind = value.annotationType();
            boolean enabled = (Boolean) kind.getMethod("enabled").invoke(value);
            LogPolicyLevel declaredLevel = (LogPolicyLevel) kind.getMethod("level").invoke(value);
            LogLevel level = declaredLevel.getLogLevel();
            Long threshold = null;
            if (value instanceof SlowLog) {
                long declaredThreshold = ((SlowLog) value).threshold();
                if (declaredThreshold < -1) {
                    throw new IllegalArgumentException("SlowLog threshold must be -1 (inherit) or zero or greater");
                }
                if (declaredThreshold != -1) {
                    threshold = declaredThreshold;
                }
            }
            List<String> allowlist = null;
            if (value instanceof RequestHeadersLog) {
                allowlist = allowlist(((RequestHeadersLog) value).allowlist());
            } else if (value instanceof ResponseHeadersLog) {
                allowlist = allowlist(((ResponseHeadersLog) value).allowlist());
            }
            if (inherited != null) {
                level = level == null ? inherited.level : level;
                threshold = threshold == null ? inherited.threshold : threshold;
                allowlist = allowlist == null ? inherited.allowlist : allowlist;
            }
            return new FeatureOverride(enabled, level, threshold, allowlist);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect logging annotation", error);
        }
    }

    private static List<String> allowlist(String[] declared) {
        if (declared.length == 1 && declared[0].isEmpty()) {
            return null;
        }
        for (String name : declared) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Header allowlist inheritance marker cannot be mixed with header names");
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(declared)));
    }

    private static boolean automaticSource(Method specific, Method original, Class<?> type) {
        if (AnnotatedElementUtils.hasAnnotation(type, RestController.class)
                || AnnotatedElementUtils.hasAnnotation(type, ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(specific, ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(original, ResponseBody.class)) {
            return true;
        }
        String feign = "org.springframework.cloud.openfeign.FeignClient";
        if (MergedAnnotations.from(type, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).isPresent(feign)
                || MergedAnnotations.from(original.getDeclaringClass(),
                        MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).isPresent(feign)) {
            return true;
        }
        MergedAnnotations annotations = MergedAnnotations.from(specific, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        return annotations.isPresent("org.springframework.scheduling.annotation.Scheduled")
                || annotations.isPresent("org.springframework.scheduling.annotation.Schedules")
                || annotations.isPresent("com.xxl.job.core.handler.annotation.XxlJob");
    }

    private static Map<Class<? extends Annotation>, InvocationLogFeature> features() {
        Map<Class<? extends Annotation>, InvocationLogFeature> features = new LinkedHashMap<>();
        features.put(EntryArgs.class, InvocationLogFeature.ENTRY_ARGS);
        features.put(ExitArgs.class, InvocationLogFeature.EXIT_ARGS);
        features.put(ExitResult.class, InvocationLogFeature.EXIT_RESULT);
        features.put(CostLog.class, InvocationLogFeature.COST_LOG);
        features.put(SlowLog.class, InvocationLogFeature.SLOW_LOG);
        features.put(ErrorLog.class, InvocationLogFeature.ERROR_LOG);
        features.put(RequestHeadersLog.class, InvocationLogFeature.REQUEST_HEADERS);
        features.put(ResponseHeadersLog.class, InvocationLogFeature.RESPONSE_HEADERS);
        return features;
    }

    private static final class MethodCache {
        private final ConcurrentMap<Method, Metadata> methods = new ConcurrentHashMap<>();
        // Spring 5 的参数名发现器也缓存类，随目标类一起管理生命周期。
        private final DefaultParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();
    }

    static final class Metadata {
        final boolean profile;
        final boolean ignored;
        final boolean automaticSource;
        final boolean voidResult;
        final String[] parameterNames;
        final Map<InvocationLogFeature, FeatureOverride> annotations;

        Metadata(boolean profile, boolean ignored, boolean automaticSource, boolean voidResult,
                String[] parameterNames, Map<InvocationLogFeature, FeatureOverride> annotations) {
            this.profile = profile;
            this.ignored = ignored;
            this.automaticSource = automaticSource;
            this.voidResult = voidResult;
            this.parameterNames = parameterNames;
            this.annotations = annotations;
        }
    }

    static final class FeatureOverride {
        final boolean enabled;
        final LogLevel level;
        final Long threshold;
        final List<String> allowlist;

        FeatureOverride(boolean enabled, LogLevel level, Long threshold, List<String> allowlist) {
            this.enabled = enabled;
            this.level = level;
            this.threshold = threshold;
            this.allowlist = allowlist;
        }
    }
}
