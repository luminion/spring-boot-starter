package io.github.luminion.velo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
