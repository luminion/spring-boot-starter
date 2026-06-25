package io.github.luminion.velo.cache;

import org.springframework.data.redis.cache.RedisCacheWriter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 为 {@link RedisCacheWriter} 提供"每个 key 写入时独立 TTL 抖动"的能力。
 * <p>
 * 通过 JDK 动态代理包装真实的 {@code RedisCacheWriter}，仅拦截写入类方法
 * （{@code put} / {@code putIfAbsent} / {@code store}）中类型为 {@link Duration} 的 TTL 参数，
 * 对其叠加 ±jitterPercentage 的随机偏移，其余方法全部原样委托。
 * <p>
 * 采用代理而非直接实现接口，是为了兼容 Spring Boot 2 / 3 / 4 之间
 * {@code RedisCacheWriter} 接口方法签名的差异，避免因接口新增抽象方法导致 {@code AbstractMethodError}。
 * <p>
 * 与按 cacheName 预先计算的抖动不同，这里在每次写入时对该条目的 TTL 单独抖动，
 * 因此同一缓存名称下的不同 key 也会获得不同的过期时间，可缓解同类型缓存批量同时过期的问题。
 *
 * @author luminion
 */
public final class JitterRedisCacheWriter {

    private JitterRedisCacheWriter() {
    }

    /**
     * 包装一个 {@link RedisCacheWriter}，使其写入时对 TTL 应用每 key 随机抖动。
     *
     * @param delegate         真实的 cache writer
     * @param jitterPercentage 抖动百分比；小于等于 0 时直接返回原始 writer，不做包装
     * @return 包装后的 writer（或在无需抖动时返回原始 writer）
     */
    public static RedisCacheWriter wrap(RedisCacheWriter delegate, int jitterPercentage) {
        if (jitterPercentage <= 0) {
            return delegate;
        }
        return (RedisCacheWriter) Proxy.newProxyInstance(
                RedisCacheWriter.class.getClassLoader(),
                new Class<?>[]{RedisCacheWriter.class},
                new JitterInvocationHandler(delegate, jitterPercentage));
    }

    /**
     * 对单个 TTL 应用随机抖动。{@code null} 或非正 TTL（永不过期）保持不变。
     */
    static Duration jitter(Duration ttl, int jitterPercentage) {
        if (ttl == null || ttl.isZero() || ttl.isNegative() || jitterPercentage <= 0) {
            return ttl;
        }
        double factor = 1.0 + (ThreadLocalRandom.current().nextDouble(-1.0, 1.0) * jitterPercentage / 100.0);
        long jitteredMillis = Math.max(1, (long) (ttl.toMillis() * factor));
        return Duration.ofMillis(jitteredMillis);
    }

    private static final class JitterInvocationHandler implements InvocationHandler {

        private final RedisCacheWriter delegate;
        private final int jitterPercentage;

        private JitterInvocationHandler(RedisCacheWriter delegate, int jitterPercentage) {
            this.delegate = delegate;
            this.jitterPercentage = jitterPercentage;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            // 仅对写入类方法的 Duration 参数做抖动；其它方法原样委托。
            if (args != null && (name.equals("put") || name.equals("putIfAbsent") || name.equals("store"))) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Duration) {
                        args[i] = jitter((Duration) args[i], jitterPercentage);
                    }
                }
            }
            try {
                return method.invoke(delegate, args);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }
}
