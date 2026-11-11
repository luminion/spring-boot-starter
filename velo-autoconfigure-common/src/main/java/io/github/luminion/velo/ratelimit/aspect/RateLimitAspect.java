package io.github.luminion.velo.ratelimit.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.AnnotatedMethodPointcut;
import io.github.luminion.velo.core.VeloMessageResolver;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import io.github.luminion.velo.spi.Fingerprinter;
import io.github.luminion.velo.util.ConcurrencyAnnotationUtils;
import io.github.luminion.velo.util.InvocationAnnotationResolver;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.util.StringUtils;

/**
 * 限流拦截器；方法上的声明优先于类型上的声明。
 *
 * @author luminion
 */
public class RateLimitAspect implements MethodInterceptor {
    private static final InvocationAnnotationResolver<RateLimit> ANNOTATIONS =
            new InvocationAnnotationResolver<>(RateLimit.class, true);

    private final String prefix;
    private final Fingerprinter fingerprinter;
    private final RateLimitHandler rateLimitHandler;
    private final VeloMessageResolver messageResolver;
    private final String defaultMessage;

    public RateLimitAspect(String prefix,
                           Fingerprinter fingerprinter,
                           RateLimitHandler rateLimitHandler) {
        this(prefix, fingerprinter, rateLimitHandler, null);
    }

    public RateLimitAspect(String prefix,
                           Fingerprinter fingerprinter,
                           RateLimitHandler rateLimitHandler,
                           VeloMessageResolver messageResolver) {
        this(prefix, fingerprinter, rateLimitHandler, messageResolver,
                new VeloProperties.RateLimitProperties().getMessage());
    }

    /**
     * 指定默认提示信息；仅在限流被拒绝时选择文案并按当前语言解析。
     */
    public RateLimitAspect(String prefix,
                           Fingerprinter fingerprinter,
                           RateLimitHandler rateLimitHandler,
                           VeloMessageResolver messageResolver, String defaultMessage) {
        this.prefix = prefix;
        this.fingerprinter = fingerprinter;
        this.rateLimitHandler = rateLimitHandler;
        this.messageResolver = messageResolver;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 限流拦截器的切点：实现方法或声明该方法的类型上直接声明了 {@link RateLimit}，不搜索父接口、父类。
     *
     * @return 静态切点
     */
    public static Pointcut pointcut() {
        return AnnotatedMethodPointcut.forDeclaredMethodsOrTypes(RateLimit.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        RateLimit rateLimit = ANNOTATIONS.resolve(invocation.getMethod(), target);
        if (rateLimit == null) {
            return invocation.proceed();
        }
        Method method = ConcurrencyAnnotationUtils.resolveSpecificMethod(target, invocation.getMethod());
        int qps = rateLimit.qps();
        if (qps <= 0) {
            throw new IllegalArgumentException("Rate limit qps must be greater than zero.");
        }
        String keyFingerprint = fingerprinter.resolveMethodFingerprint(
                target, method, invocation.getArguments(), rateLimit.prefix(), rateLimit.value());
        String key = ConcurrencyAnnotationUtils.buildPrefixedKey(prefix, keyFingerprint);

        if (!rateLimitHandler.tryAcquire(key, qps)) {
            throw new RateLimitException(resolveMessage(rateLimit.message()), key, qps);
        }
        return invocation.proceed();
    }

    private String resolveMessage(String message) {
        // 非空白注解文案覆盖全局配置；两种来源都沿用统一的国际化解析。
        String selectedMessage = StringUtils.hasText(message) ? message : defaultMessage;
        return messageResolver != null ? messageResolver.resolve(selectedMessage) : selectedMessage;
    }
}
