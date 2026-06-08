package io.github.luminion.velo.log.trace;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Trace id helpers backed by SLF4J MDC.
 */
public final class TraceContext {

    private TraceContext() {
    }

    public static String get(String mdcKey) {
        if (!StringUtils.hasText(mdcKey)) {
            return null;
        }
        return MDC.get(mdcKey);
    }

    public static void put(String mdcKey, String traceId) {
        if (!StringUtils.hasText(mdcKey) || !StringUtils.hasText(traceId)) {
            return;
        }
        MDC.put(mdcKey, traceId);
    }

    public static void remove(String mdcKey) {
        if (StringUtils.hasText(mdcKey)) {
            MDC.remove(mdcKey);
        }
    }

    public static String createTraceId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return String.format("%016x%016x", random.nextLong(), random.nextLong());
    }
}
