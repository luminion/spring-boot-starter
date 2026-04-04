package io.github.luminion.velo.core;

import io.github.luminion.velo.core.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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

        assertThat(resolved).isEqualTo(SampleService.class.getName() + "#execute");
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

    static class SampleService {
        void execute(String userId, int version) {
        }
    }
}
