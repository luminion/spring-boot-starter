package io.github.luminion.velo;

/**
 * Backend implementation used by concurrency-related features.
 */
public enum ConcurrencyBackend {

    /**
     * Selects the first available backend by auto-configuration order.
     */
    AUTO,

    /**
     * Redisson based implementation.
     */
    REDISSON,

    /**
     * Spring Data Redis based implementation.
     */
    REDIS,

    /**
     * Caffeine based local implementation.
     */
    CAFFEINE,

    /**
     * JDK based local implementation.
     */
    JDK
}
