package io.github.luminion.velo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 验证并发控制注解的键前缀归一化与指纹回退。 */
class ConcurrencyAnnotationUtilsTest {

    @Test
    void shouldNormalizeTrailingPrefixSeparators() {
        assertEquals("rateLimit:fingerprint", ConcurrencyAnnotationUtils.buildPrefixedKey("rateLimit:", "fingerprint"));
        assertEquals("lock:fingerprint", ConcurrencyAnnotationUtils.buildPrefixedKey("lock::", "fingerprint"));
    }

    @Test
    void shouldFallbackToFingerprintWhenPrefixBlank() {
        assertEquals("fingerprint", ConcurrencyAnnotationUtils.buildPrefixedKey("", "fingerprint"));
        assertEquals("fingerprint", ConcurrencyAnnotationUtils.buildPrefixedKey("::", "fingerprint"));
    }
}
