package io.github.luminion.velo.core.util;

import org.springframework.util.StringUtils;

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
}
