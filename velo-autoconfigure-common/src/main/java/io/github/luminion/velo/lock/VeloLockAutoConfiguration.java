package io.github.luminion.velo.lock;

import io.github.luminion.velo.ConcurrencyBackend;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.lock.config.VeloLockJdkAutoConfiguration;
import io.github.luminion.velo.lock.config.VeloLockRedisConfiguration;
import io.github.luminion.velo.lock.config.VeloLockRedissonAutoConfiguration;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.lock.aspect.LockAspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import io.github.luminion.velo.core.VeloAdvisorOrder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;

/**
 * 锁自动配置实现。
 */
@AutoConfiguration(afterName = "io.github.luminion.velo.lock.config.VeloLockRedisAutoConfiguration", after = {
        VeloLockRedissonAutoConfiguration.class,
        VeloLockRedisConfiguration.class,
        VeloLockJdkAutoConfiguration.class
})
@ConditionalOnProperty(prefix = "velo.lock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VeloLockAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LockHandler.class)
    public LockHandler unavailableLockHandler(VeloProperties properties) {
        ConcurrencyBackend backend = properties.getLock().getBackend();
        if (backend != ConcurrencyBackend.AUTO) {
            throw new IllegalStateException("No LockHandler is available for velo.lock.backend=" + backend
                    + ". Supported backends: REDISSON, REDIS, JDK, or a custom LockHandler.");
        }
        return new LockHandler() {
            @Override
            public boolean tryLock(String key) {
                throw new IllegalStateException("No LockHandler is available. Configure a supported backend "
                        + "or provide a custom LockHandler.");
            }

            @Override
            public void unlock(String key) {
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(LockAspect.class)
    @ConditionalOnBean({Fingerprinter.class, LockHandler.class})
    public LockAspect lockAspect(VeloProperties properties, Fingerprinter fingerprinter, LockHandler lockHandler,
            ObjectProvider<VeloMessageResolver> messageResolver) {
        return new LockAspect(properties.getLock().getPrefix(), fingerprinter, lockHandler,
                messageResolver.getIfAvailable(),
                properties.getLock().getMessage());
    }

    /**
     * 只携带切点和顺序的基础设施 Advisor；拦截器在容器单例初始化完成后解析，避免提前创建业务依赖。
     */
    @Bean(name = "lockAdvisor")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "lockAdvisor")
    @ConditionalOnBean(LockAspect.class)
    public static DeferredInterceptorAdvisor lockAdvisor(Environment environment, ObjectProvider<LockAspect> lockAspect) {
        int order = Binder.get(environment).bind("velo.aspect-order.lock", Integer.class).orElse(VeloAdvisorOrder.CONCURRENCY_LOCK);
        return new DeferredInterceptorAdvisor(LockAspect.pointcut(), order, lockAspect::getObject);
    }
}
