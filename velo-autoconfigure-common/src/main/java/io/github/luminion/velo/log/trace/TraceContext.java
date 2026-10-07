package io.github.luminion.velo.log.trace;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

/**
 * 链路快照和 MDC 的作用域管理；协议解析由可替换的 Resolver 完成。
 */
@Slf4j
public final class TraceContext {
    private static final int MAX_TRACE_ID_LENGTH = 128;
    private static final ThreadLocal<TraceData> CURRENT = new ThreadLocal<>();
    private static final TraceContextResolver DEFAULT_RESOLVER = new W3cTraceContextResolver();

    private TraceContext() {
    }

    public static String get(String key) {
        return StringUtils.hasText(key) ? MDC.get(key) : null;
    }

    public static void put(String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            MDC.put(key, value);
        }
    }

    public static void remove(String key) {
        if (StringUtils.hasText(key)) {
            MDC.remove(key);
        }
    }

    public static void restore(String key, String previous) {
        if (StringUtils.hasText(key)) {
            if (previous == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, previous);
            }
        }
    }

    public static boolean isValid(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_TRACE_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean safe =
                    c >= 'a' && c <= 'z'
                            || c >= 'A' && c <= 'Z'
                            || c >= '0' && c <= '9'
                            || c == '-'
                            || c == '_'
                            || c == '.';
            if (!safe) {
                return false;
            }
        }
        return true;
    }

    public static String resolveInbound(String candidate) {
        return isValid(candidate) ? candidate : createTraceId();
    }

    public static String createTraceId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long high;
        long low;
        do {
            high = random.nextLong();
            low = random.nextLong();
        } while (high == 0 && low == 0);
        return String.format("%016x%016x", high, low);
    }

    static String createParentId() {
        long value;
        do {
            value = ThreadLocalRandom.current().nextLong();
        } while (value == 0);
        return String.format("%016x", value);
    }

    /**
     * 无上下文时移除 get() 初始化的空关联。
     */
    public static TraceData current() {
        TraceData data = CURRENT.get();
        if (data == null) {
            CURRENT.remove();
        }
        return data;
    }

    /**
     * 新 HTTP 入口不沿用工作线程的残留数据，Resolver 可以读取当前请求。
     */
    public static TraceData resolveInbound(String key, TraceContextResolver resolver) {
        try (Scope ignored = install(key, (TraceData) null, true)) {
            return resolveSafely(resolver);
        }
    }

    /**
     * 独立任务不沿用调用方请求；用户自定义 ThreadLocal 仍由其实现自行管理。
     */
    public static Scope root(String key, boolean enabled, TraceContextResolver resolver) {
        if (!enabled) {
            return install(key, current(), false);
        }
        TraceData data;
        try (Scope ignored = install(key, (TraceData) null, true)) {
            data = CurrentRequestHeaders.withoutRequest(() -> resolveSafely(resolver));
        }
        return install(key, data, true);
    }

    /**
     * 有上下文则复用；无上下文时只在当前作用域生成，关闭时恢复。
     */
    public static Scope open(String key, boolean enabled) {
        return open(key, enabled, DEFAULT_RESOLVER);
    }

    public static Scope open(String key, boolean enabled, TraceContextResolver resolver) {
        if (!enabled) {
            return install(key, current(), false);
        }
        TraceData data = current();
        String existing = get(key);
        if (data == null || !Objects.equals(existing, data.getTraceId())) {
            // 兼容只写 MDC 的调用方；具体实现决定该标识能否用于自己的协议。
            TraceData seed = isValid(existing) ? new TraceData(existing, Collections.emptyMap()) : null;
            try (Scope ignored = install(key, seed, true)) {
                data = resolveSafely(resolver);
            }
        }
        return install(key, data, true);
    }

    private static TraceData resolveSafely(TraceContextResolver resolver) {
        try {
            TraceData data = resolver.resolve();
            if (data != null) {
                return data;
            }
        } catch (RuntimeException error) {
            log.warn("Cannot resolve trace context; creating a fallback context");
        }
        return CurrentRequestHeaders.withoutRequest(DEFAULT_RESOLVER::resolve);
    }

    /**
     * 临时安装完整快照，关闭时同时恢复链路数据和 MDC。
     */
    public static Scope install(String key, TraceData value, boolean enabled) {
        return new Scope(key, value, enabled);
    }

    public static final class Scope implements AutoCloseable {
        private final String key;
        private final String previous;
        private final TraceData previousData;
        private final boolean enabled;

        private Scope(String key, TraceData current, boolean enabled) {
            this.key = key;
            this.previous = get(key);
            this.previousData = TraceContext.current();
            this.enabled = enabled;
            if (enabled) {
                setCurrent(current);
                restore(key, current == null ? null : current.getTraceId());
            }
        }

        @Override
        public void close() {
            if (enabled) {
                setCurrent(previousData);
                restore(key, previous);
            }
        }

        private static void setCurrent(TraceData value) {
            if (value == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(value);
            }
        }
    }
}
