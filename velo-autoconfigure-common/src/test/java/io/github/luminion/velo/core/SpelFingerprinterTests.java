package io.github.luminion.velo;

import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpelFingerprinterTests {

    private final SpelFingerprinter fingerprinter = new SpelFingerprinter();

    @Test
    void shouldResolveExplicitSpelKeyWithoutMethodPrefix() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, int.class);

        String resolved = fingerprinter.resolveMethodFingerprint(
                new SampleService(),
                method,
                new Object[]{"user-1", 7},
                "#p0 + ':' + #p1");

        assertThat(resolved).isEqualTo("user-1:7");
    }

    @Test
    void shouldFallbackToMethodFingerprintWhenExpressionBlank() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, int.class);

        String resolved = fingerprinter.resolveMethodFingerprint(
                new SampleService(),
                method,
                new Object[]{"user-1", 7},
                "");

        assertThat(resolved).isEqualTo(SampleService.class.getName() + "#execute(java.lang.String,int)");
    }

    @Test
    void shouldDistinguishOverloadedMethods() throws NoSuchMethodException {
        Method stringMethod = OverloadedService.class.getDeclaredMethod("execute", String.class);
        Method longMethod = OverloadedService.class.getDeclaredMethod("execute", Long.class);

        String stringFingerprint = fingerprinter.resolveMethodFingerprint(
                new OverloadedService(), stringMethod, new Object[]{"value"}, "");
        String longFingerprint = fingerprinter.resolveMethodFingerprint(
                new OverloadedService(), longMethod, new Object[]{1L}, "");

        assertThat(stringFingerprint)
                .isEqualTo(OverloadedService.class.getName() + "#execute(java.lang.String)");
        assertThat(longFingerprint)
                .isEqualTo(OverloadedService.class.getName() + "#execute(java.lang.Long)");
        assertThat(stringFingerprint).isNotEqualTo(longFingerprint);
    }

    @Test
    void shouldRejectBlankSpelKeyResult() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, int.class);

        assertThatThrownBy(() -> fingerprinter.resolveMethodFingerprint(
                new SampleService(),
                method,
                new Object[]{"user-1", 7},
                "'   '"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolved to a blank value");
    }

    @Test
    void shouldRejectNullSpelKeyResult() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, int.class);

        assertThatThrownBy(() -> fingerprinter.resolveMethodFingerprint(
                new SampleService(),
                method,
                new Object[]{null, 7},
                "#p0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resolved to null");
    }

    @Test
    void shouldEvictOldSpelExpressionsWhenCacheReachesLimit() throws Exception {
        Method method = SampleService.class.getDeclaredMethod("execute", String.class, int.class);
        Field cacheField = SpelFingerprinter.class.getDeclaredField("EXPRESSION_CACHE");
        cacheField.setAccessible(true);
        Map<?, ?> expressionCache = (Map<?, ?>) cacheField.get(null);
        synchronized (expressionCache) {
            expressionCache.clear();
        }

        for (int i = 0; i < 300; i++) {
            fingerprinter.resolveMethodFingerprint(
                    new SampleService(),
                    method,
                    new Object[]{"user-" + i, 7},
                    "#p0 + ':' + " + i);
        }

        synchronized (expressionCache) {
            assertThat(expressionCache).hasSize(256);
        }
    }

    static class SampleService {
        void execute(String userId, int version) {
        }
    }

    static class OverloadedService {
        void execute(String value) {
        }

        void execute(Long value) {
        }
    }
}
