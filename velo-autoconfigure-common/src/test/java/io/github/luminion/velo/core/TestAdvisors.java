package io.github.luminion.velo.core;

import io.github.luminion.velo.feign.FeignLogAspect;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.log.aspect.XxlJobLogAspect;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * 测试中按生产切点包装 Starter 拦截器，代理链顺序与添加顺序一致。
 *
 * @author luminion
 */
public final class TestAdvisors {
    private TestAdvisors() {
    }

    public static Advisor of(Object interceptor) {
        return new DefaultPointcutAdvisor(pointcut(interceptor), (MethodInterceptor) interceptor);
    }

    private static Pointcut pointcut(Object interceptor) {
        if (interceptor instanceof LockAspect) {
            return LockAspect.pointcut();
        }
        if (interceptor instanceof RateLimitAspect) {
            return RateLimitAspect.pointcut();
        }
        if (interceptor instanceof IdempotentAspect) {
            return IdempotentAspect.pointcut();
        }
        if (interceptor instanceof InvokeLogAspect) {
            return InvokeLogAspect.pointcut();
        }
        if (interceptor instanceof ScheduledLogAspect) {
            return ScheduledLogAspect.pointcut();
        }
        if (interceptor instanceof XxlJobLogAspect) {
            return XxlJobLogAspect.pointcut();
        }
        if (interceptor instanceof FeignLogAspect) {
            return FeignLogAspect.pointcut();
        }
        throw new IllegalArgumentException("Unsupported interceptor: " + interceptor);
    }
}
