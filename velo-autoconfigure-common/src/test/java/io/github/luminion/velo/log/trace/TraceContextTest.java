package io.github.luminion.velo.log.trace;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class TraceContextTest {

    @Test
    void shouldAcceptSafeTraceId() {
        assertThat(TraceContext.isValid("abc-123_ok.v2")).isTrue();
    }

    @Test
    void shouldRejectEmptyOverlongOrUnsafeTraceId() {
        assertThat(TraceContext.isValid(null)).isFalse();
        assertThat(TraceContext.isValid("")).isFalse();
        // 含换行符，典型日志注入载荷
        assertThat(TraceContext.isValid("bad\ninjected")).isFalse();
        // 含空格
        assertThat(TraceContext.isValid("has space")).isFalse();
        // 超过 128 字符上限
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 129; i++) {
            tooLong.append('a');
        }
        assertThat(TraceContext.isValid(tooLong.toString())).isFalse();
    }

    @Test
    void shouldKeepValidInboundButRegenerateInvalid() {
        assertThat(TraceContext.resolveInbound("valid-trace-id")).isEqualTo("valid-trace-id");
        // 非法入站被替换为新生成的 32 位 hex
        String regenerated = TraceContext.resolveInbound("bad value\n");
        assertThat(regenerated).hasSize(32).matches("[0-9a-f]+");
    }

    @Test
    void shouldRestorePreviousMdcValueInsteadOfClearing() {
        String key = "traceId";
        MDC.put(key, "upstream-value");
        try {
            String previous = TraceContext.get(key);
            TraceContext.put(key, "inner-value");
            assertThat(TraceContext.get(key)).isEqualTo("inner-value");

            TraceContext.restore(key, previous);
            // 恢复到上游值而非清空
            assertThat(TraceContext.get(key)).isEqualTo("upstream-value");
        } finally {
            MDC.remove(key);
        }
    }

    @Test
    void shouldRemoveWhenPreviousWasAbsent() {
        String key = "traceId";
        String previous = TraceContext.get(key);
        TraceContext.put(key, "inner-value");
        TraceContext.restore(key, previous);
        // 之前无值时应清除，不残留
        assertThat(TraceContext.get(key)).isNull();
    }
}
