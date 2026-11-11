package io.github.luminion.velo.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.feign.FeignLogAspect;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.velo.util.InvocationAnnotationResolver;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 验证内置切点的匹配范围：直接声明与层级查找的边界、Object 方法排除及 Feign 代理识别。
 *
 * @author luminion
 */
class AnnotatedMethodPointcutTests {
    @Test
    void concurrencyPointcutsOnlyMatchDirectlyDeclaredAnnotations() {
        assertThat(AopUtils.canApply(LockAspect.pointcut(), DirectLockService.class)).isTrue();
        assertThat(AopUtils.canApply(LockAspect.pointcut(), InheritedLockService.class)).isFalse();
        assertThat(AopUtils.canApply(IdempotentAspect.pointcut(), DirectLockService.class)).isFalse();
        assertThat(AopUtils.canApply(IdempotentAspect.pointcut(), IdempotentService.class)).isTrue();
    }

    @Test
    void classLevelRateLimitOnlyAppliesToMethodsDeclaredInAnnotatedClass() throws Exception {
        MethodMatcher matcher = RateLimitAspect.pointcut().getMethodMatcher();
        Method inherited = RateLimitedBase.class.getMethod("base");
        Method declared = PlainSubclass.class.getMethod("own");
        assertThat(matcher.matches(inherited, PlainSubclass.class)).isTrue();
        assertThat(matcher.matches(declared, PlainSubclass.class)).isFalse();
        assertThat(matcher.matches(Object.class.getMethod("toString"), RateLimitedBase.class)).isFalse();
        assertThat(matcher.matches(RateLimitedBase.class.getMethod("hashCode"), RateLimitedBase.class)).isFalse();

        InvocationAnnotationResolver<RateLimit> resolver = new InvocationAnnotationResolver<>(RateLimit.class, true);
        assertThat(resolver.resolve(inherited, new PlainSubclass()).qps()).isEqualTo(7);
        assertThat(resolver.resolve(RateLimitedBase.class.getMethod("overridden"), new RateLimitedBase()).qps())
                .isEqualTo(3);
        assertThat(resolver.resolve(declared, new PlainSubclass())).isNull();
    }

    @Test
    void hierarchyPointcutFindsInterfaceDeclarationsAndSkipsUnproxiableMethods() throws Exception {
        Pointcut pointcut = ScheduledLogAspect.pointcut();
        assertThat(AopUtils.canApply(pointcut, ScheduledJob.class)).isTrue();
        assertThat(pointcut.getMethodMatcher().matches(ScheduledJob.class.getDeclaredMethod("hidden"), ScheduledJob.class))
                .isFalse();
        assertThat(pointcut.getClassFilter().matches(String.class)).isFalse();
    }

    @Test
    void feignPointcutMatchesClientProxiesButNotFallbackImplementations() {
        Object client = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{UserClient.class},
                (proxy, method, args) -> null);
        assertThat(AopUtils.canApply(FeignLogAspect.pointcut(), client.getClass())).isTrue();
        assertThat(AopUtils.canApply(FeignLogAspect.pointcut(), UserClientFallback.class)).isFalse();
    }

    static class DirectLockService {
        @Lock
        public void run() {
        }
    }

    interface LockedContract {
        @Lock
        void run();
    }

    static class InheritedLockService implements LockedContract {
        @Override
        public void run() {
        }
    }

    static class IdempotentService {
        @Idempotent
        public void submit() {
        }
    }

    @RateLimit(qps = 7)
    static class RateLimitedBase {
        public void base() {
        }

        @RateLimit(qps = 3)
        public void overridden() {
        }
    }

    static class PlainSubclass extends RateLimitedBase {
        public void own() {
        }
    }

    interface Job {
        @Scheduled(fixedDelay = 1000)
        void run();
    }

    static class ScheduledJob implements Job {
        @Override
        public void run() {
        }

        @Scheduled(fixedDelay = 1000)
        private void hidden() {
        }
    }

    interface UserApi {
        String find(String id);
    }

    @FeignClient(name = "users")
    interface UserClient extends UserApi {
    }

    static class UserClientFallback implements UserClient {
        @Override
        public String find(String id) {
            return "fallback";
        }
    }
}
