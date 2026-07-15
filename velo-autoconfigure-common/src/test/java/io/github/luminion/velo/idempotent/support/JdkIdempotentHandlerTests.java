package io.github.luminion.velo.idempotent.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class JdkIdempotentHandlerTests {

    @Test
    void shouldUseMonotonicNanosecondsForMillisecondTtl() {
        AtomicLong nanoTime = new AtomicLong(1L);
        JdkIdempotentHandler handler = new JdkIdempotentHandler(nanoTime::get);

        try {
            assertThat(handler.tryRecord("order:1", "first", 1L)).isTrue();
            assertThat(handler.tryRecord("order:1", "second", 1L)).isFalse();

            nanoTime.addAndGet(999_999L);
            assertThat(handler.tryRecord("order:1", "third", 1L)).isFalse();

            nanoTime.incrementAndGet();
            assertThat(handler.tryRecord("order:1", "fourth", 1L)).isTrue();
        } finally {
            handler.destroy();
        }
    }
}
