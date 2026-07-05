package io.github.luminion.velo.lock.config;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.condition.ConditionalOnConcurrencyBackend;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.support.JdkLockHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 锁自动配置 (Caffeine 档位)
 * <p>
 * Caffeine 是纯缓存库、不提供任何互斥锁 API，本地锁只能靠 {@code ConcurrentHashMap + ReentrantLock} 实现，
 * 与 JDK 档位完全一致。因此这里不再维护单独的 CaffeineLockHandler，CAFFEINE 档位直接复用 {@link JdkLockHandler}，
 * 只保留一份更简单的本地实现。保留该档位是为了让显式 {@code velo.lock.backend=CAFFEINE} 仍可用，不破坏兼容。
 *
 * @author luminion
 */
@AutoConfiguration(after = VeloLockRedisAutoConfiguration.class)
@ConditionalOnConcurrencyBackend(prefix = "velo.lock", value = ConcurrencyBackend.CAFFEINE,
        autoClassNames = "org.aspectj.weaver.Advice")
@ConditionalOnMissingBean(LockHandler.class)
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockCaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler lockHandler() {
        return new JdkLockHandler();
    }
}
