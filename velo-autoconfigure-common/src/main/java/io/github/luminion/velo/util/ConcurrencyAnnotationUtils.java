package io.github.luminion.velo.util;

import org.springframework.aop.support.AopUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 并发控制注解属性解析工具。
 */
public final class ConcurrencyAnnotationUtils {

    private ConcurrencyAnnotationUtils() {
    }

    public static String buildPrefixedKey(String prefix, String fingerprint) {
        if (!StringUtils.hasText(prefix)) {
            return fingerprint;
        }
        String normalizedPrefix = prefix.trim();
        while (normalizedPrefix.endsWith(":")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        if (!StringUtils.hasText(normalizedPrefix)) {
            return fingerprint;
        }
        return normalizedPrefix + ':' + fingerprint;
    }

    public static Method resolveSpecificMethod(Object target, Method method) {
        Class<?> targetClass = target != null ? AopUtils.getTargetClass(target) : method.getDeclaringClass();
        return AopUtils.getMostSpecificMethod(method, targetClass);
    }

    public static String requireKeyExpression(String featureName, String expression) {
        if (!StringUtils.hasText(expression)) {
            throw new IllegalArgumentException(featureName + " key must be provided and must not be blank.");
        }
        return expression;
    }
}
