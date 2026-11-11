package io.github.luminion.velo.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.config.VeloLogAutoConfiguration;
import io.github.luminion.velo.log.config.VeloSourceLogAutoConfiguration;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.spi.InvocationLogWriter;
import io.github.luminion.velo.trace.VeloTraceAutoConfiguration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ClassUtils;

/**
 * 本模块的类路径不包含 AspectJ：验证 Starter 的拦截能力不依赖 AspectJ。
 * <p>
 * Spring Boot 在缺少 AspectJ 时注册 {@link InfrastructureAdvisorAutoProxyCreator}，
 * 它只应用基础设施角色的 Advisor；Starter 的 Advisor 均声明为基础设施角色，因此仍能生效。
 */
class OptionalAspectJAutoConfigurationTests {

    @ParameterizedTest
    @ValueSource(strings = {
            "io.github.luminion.velo.log.config.VeloLogAutoConfiguration",
            "io.github.luminion.velo.log.config.VeloSourceLogAutoConfiguration",
            "io.github.luminion.velo.lock.VeloLockAutoConfiguration",
            "io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration",
            "io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration",
            "io.github.luminion.velo.feign.VeloFeignAutoConfiguration"
    })
    void configurationsAreInspectableWithoutAspectJ(String configurationName) throws Exception {
        try (FilteredClassLoader classLoader = new FilteredClassLoader("org.aspectj")) {
            Class<?> configuration = Class.forName(configurationName, false, classLoader);
            assertThat(configuration.getDeclaredAnnotations()).isNotEmpty();
        }
    }

    @Test
    void starterAdvisorsApplyWithoutAspectJ() {
        // 使用真实类路径；本模块不包含 AspectJ。
        assertThat(ClassUtils.isPresent("org.aspectj.weaver.Advice", getClass().getClassLoader())).isFalse();
        List<InvocationLogRecord> records = new CopyOnWriteArrayList<>();
        AtomicInteger locks = new AtomicInteger();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        VeloCoreAutoConfiguration.class,
                        VeloTraceAutoConfiguration.class,
                        VeloLogAutoConfiguration.class,
                        VeloSourceLogAutoConfiguration.class,
                        VeloLockAutoConfiguration.class))
                .withBean(InvocationLogWriter.class, () -> records::add)
                .withBean(LockHandler.class, () -> new LockHandler() {
                    @Override
                    public boolean tryLock(String key) {
                        locks.incrementAndGet();
                        return true;
                    }

                    @Override
                    public void unlock(String key) {
                    }
                })
                .withBean(Operations.class, Operations::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME))
                            .isExactlyInstanceOf(InfrastructureAdvisorAutoProxyCreator.class);
                    Operations operations = context.getBean(Operations.class);
                    assertThat(AopUtils.isAopProxy(operations)).isTrue();

                    assertThat(operations.locked()).isEqualTo("locked");
                    assertThat(locks).hasValue(1);

                    assertThat(operations.logged("value")).isEqualTo("logged:value");
                    assertThat(records).isNotEmpty().allSatisfy(record ->
                            assertThat(record.getSource()).isEqualTo(InvocationLogSource.ANNOTATION));
                });
    }

    static class Operations {
        @Lock
        public String locked() {
            return "locked";
        }

        @InvokeLog
        public String logged(String value) {
            return "logged:" + value;
        }
    }
}
