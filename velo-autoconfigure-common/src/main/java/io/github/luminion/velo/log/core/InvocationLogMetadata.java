package io.github.luminion.velo.log.core;

import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogPolicy;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 入口与策略解析共享的静态注解元数据；按完整功能声明覆盖并随目标类释放缓存。
 */
public final class InvocationLogMetadata {
    private static final Map<Class<? extends Annotation>, InvocationLogFeature> FEATURES = features();
    private static final ClassValue<MethodCache> CACHE = new ClassValue<MethodCache>() {
        @Override
        protected MethodCache computeValue(Class<?> type) {
            return new MethodCache();
        }
    };
    private static final ClassValue<Map<InvocationLogFeature, InvocationLogPolicy>> PRESETS =
            new ClassValue<Map<InvocationLogFeature, InvocationLogPolicy>>() {
                @Override
                protected Map<InvocationLogFeature, InvocationLogPolicy> computeValue(Class<?> type) {
                    return inspectPreset(type);
                }
            };

    private InvocationLogMetadata() {
    }

    /**
     * 功能组合可建立普通入口；自动来源和单独的 Ignore 不重复建立入口。
     */
    public static boolean isOrdinaryEntry(Method method, Class<?> targetClass) {
        Metadata metadata = get(method, targetClass);
        return !metadata.automaticSource && !metadata.annotations.isEmpty();
    }

    static Metadata get(Method method, Class<?> targetClass) {
        Class<?> type = targetClass == null ? method.getDeclaringClass() : targetClass;
        MethodCache cache = CACHE.get(type);
        return cache.methods.computeIfAbsent(method, original -> inspect(original, type, cache.names));
    }

    static Map<InvocationLogFeature, InvocationLogPolicy> preset(Class<? extends Annotation> type) {
        Class<? extends Annotation> annotationType = Objects.requireNonNull(type, "defaultAnnotation");
        return PRESETS.get(annotationType);
    }

    private static Map<InvocationLogFeature, InvocationLogPolicy> inspectPreset(Class<?> type) {
        Retention retention = type.getAnnotation(Retention.class);
        if (!type.isAnnotation() || retention == null || retention.value() != RetentionPolicy.RUNTIME) {
            throw new IllegalArgumentException("Default logging annotation must be a runtime annotation: " + type.getName());
        }
        for (Method attribute : type.getDeclaredMethods()) {
            if (attribute.getDefaultValue() == null) {
                throw new IllegalArgumentException("Default logging annotation attributes require default values: " + type.getName());
            }
        }
        // 合成默认实例后再交给 Spring 合并，保留业务组合属性的 AliasFor 默认映射。
        Class<? extends Annotation> annotationType = type.asSubclass(Annotation.class);
        Annotation value = AnnotationUtils.synthesizeAnnotation(Collections.emptyMap(), annotationType, null);
        Map<Object, Map<Class<? extends Annotation>, Annotation>> scopes = new LinkedHashMap<>();
        collect(MergedAnnotations.from(value), scopes);
        Map<InvocationLogFeature, InvocationLogPolicy> policies = new EnumMap<>(InvocationLogFeature.class);
        for (Map<Class<? extends Annotation>, Annotation> scope : scopes.values()) {
            if (scope.containsKey(LogIgnore.class)) {
                throw new IllegalArgumentException("Default logging annotation cannot contain LogIgnore: " + type.getName());
            }
            apply(scope, policies);
        }
        if (policies.isEmpty()) {
            throw new IllegalArgumentException("Default logging annotation must contain logging features: " + type.getName());
        }
        return Collections.unmodifiableMap(policies);
    }

    private static Metadata inspect(Method original, Class<?> type, DefaultParameterNameDiscoverer names) {
        Method specific = AopUtils.getMostSpecificMethod(original, type);
        Map<Object, Map<Class<? extends Annotation>, Annotation>> scopes = new LinkedHashMap<>();
        collect(specific, scopes);
        collect(original, scopes);
        collect(type, scopes);
        collect(original.getDeclaringClass(), scopes);
        boolean ignored = false;
        Map<InvocationLogFeature, InvocationLogPolicy> policies = new EnumMap<>(InvocationLogFeature.class);
        List<Map<Class<? extends Annotation>, Annotation>> ordered = new ArrayList<>(scopes.values());
        Collections.reverse(ordered);
        for (Map<Class<? extends Annotation>, Annotation> scope : ordered) {
            ignored |= scope.containsKey(LogIgnore.class);
            // 更具体的声明整体替换同项功能，不逐属性继承，其他功能继续保留。
            apply(scope, policies);
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
        return new Metadata(ignored, automaticSource, specific.getReturnType() == Void.TYPE,
                parameterNames, Collections.unmodifiableMap(policies));
    }

    private static void collect(AnnotatedElement element,
            Map<Object, Map<Class<? extends Annotation>, Annotation>> scopes) {
        MergedAnnotations annotations = MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        collect(annotations, scopes);
    }

    private static void collect(MergedAnnotations annotations,
            Map<Object, Map<Class<? extends Annotation>, Annotation>> scopes) {
        for (MergedAnnotation<Annotation> annotation : annotations) {
            Class<? extends Annotation> kind = annotation.getType();
            if (!FEATURES.containsKey(kind) && kind != LogIgnore.class) {
                continue;
            }
            Map<Class<? extends Annotation>, Annotation> scope =
                    scopes.computeIfAbsent(annotation.getSource(), ignored -> new LinkedHashMap<>());
            // Spring 同一来源按距离从近到远遍历，直接声明优先；套餐也按普通组合展开。
            if (!scope.containsKey(kind)) {
                scope.put(kind, annotation.synthesize());
            }
        }
    }

    private static void apply(Map<Class<? extends Annotation>, Annotation> scope,
            Map<InvocationLogFeature, InvocationLogPolicy> policies) {
        for (Map.Entry<Class<? extends Annotation>, Annotation> entry : scope.entrySet()) {
            InvocationLogFeature feature = FEATURES.get(entry.getKey());
            if (feature != null) {
                InvocationLogPolicy policy = policy(entry.getValue());
                policies.put(feature, policy);
            }
        }
    }

    private static InvocationLogPolicy policy(Annotation value) {
        LogPolicyLevel level = (LogPolicyLevel) AnnotationUtils.getValue(value, "level");
        Object length = AnnotationUtils.getValue(value, "maxLength");
        int maxLength = length == null ? -1 : (Integer) length;
        long threshold = value instanceof SlowLog ? ((SlowLog) value).threshold() : 1000;
        List<String> allowlist = Collections.emptyList();
        if (value instanceof RequestHeadersLog) {
            allowlist = Arrays.asList(((RequestHeadersLog) value).allowlist());
        } else if (value instanceof ResponseHeadersLog) {
            allowlist = Arrays.asList(((ResponseHeadersLog) value).allowlist());
        }
        for (String header : allowlist) {
            if (header.isEmpty()) {
                throw new IllegalArgumentException("Header allowlist names must not be empty");
            }
        }
        return new InvocationLogPolicy(level, threshold, allowlist, maxLength);
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
        private final DefaultParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();
    }

    static final class Metadata {
        final boolean ignored;
        final boolean automaticSource;
        final boolean voidResult;
        final String[] parameterNames;
        final Map<InvocationLogFeature, InvocationLogPolicy> annotations;

        Metadata(boolean ignored, boolean automaticSource, boolean voidResult,
                String[] parameterNames, Map<InvocationLogFeature, InvocationLogPolicy> annotations) {
            this.ignored = ignored;
            this.automaticSource = automaticSource;
            this.voidResult = voidResult;
            this.parameterNames = parameterNames;
            this.annotations = annotations;
        }
    }
}
