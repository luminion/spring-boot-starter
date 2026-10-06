package io.github.luminion.velo;

/**
 * 并发功能的后端类型，各功能仅支持其中的部分实现。
 */
public enum ConcurrencyBackend {

    /**
     * 按自动配置顺序选择首个可用后端。
     */
    AUTO,

    /**
     * Redisson 分布式实现。
     */
    REDISSON,

    /**
     * Spring Data Redis 分布式实现。
     */
    REDIS,

    /**
     * Caffeine 本地幂等实现。
     */
    CAFFEINE,

    /**
     * Guava 本地限流实现。
     */
    GUAVA,

    /**
     * JDK 本地锁实现。
     */
    JDK
}
