package io.github.luminion.velo.feign;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Feign bean 日志代理后处理器。
 */
public class FeignLogBeanPostProcessor implements BeanPostProcessor {

    private final VeloProperties properties;

    private final RuntimeJsonSerializer runtimeJsonSerializer;

    public FeignLogBeanPostProcessor(VeloProperties properties, RuntimeJsonSerializer runtimeJsonSerializer) {
        this.properties = properties;
        this.runtimeJsonSerializer = runtimeJsonSerializer;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanType = ClassUtils.getUserClass(bean);
        if (!FeignClientMetadataResolver.isFeignClientType(beanType)) {
            return bean;
        }
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(false);
        proxyFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
            Method method = invocation.getMethod();
            if (ReflectionUtils.isObjectMethod(method)) {
                return invocation.proceed();
            }
            Logger logger = LoggerFactory.getLogger(method.getDeclaringClass());
            LogLevel level = properties.getLog().getLevel();
            VeloProperties.FeignProperties feignProperties = properties.getFeign();
            String clientName = FeignClientMetadataResolver.resolveClientName(method.getDeclaringClass());
            FeignRequestMetadata requestMetadata = FeignClientMetadataResolver.resolveRequestMetadata(method);
            String prefix = FeignLogSupport.buildInvocationPrefix(clientName, method, requestMetadata);

            if (FeignLogSupport.isEnabled(logger, level)) {
                String argsText = FeignLogSupport.buildArgsText(method, invocation.getArguments(), runtimeJsonSerializer, feignProperties);
                FeignLogSupport.log(logger, level, "{}==> args: {}", prefix, argsText);
            }
            long start = System.nanoTime();
            try {
                Object result = invocation.proceed();
                if (FeignLogSupport.isEnabled(logger, level)) {
                    String resultText = FeignLogSupport.buildResultText(result, runtimeJsonSerializer, feignProperties);
                    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
                    FeignLogSupport.log(logger, level, "{}<== cost:{}ms, resp: {}", prefix, elapsedMs, resultText);
                }
                return result;
            } catch (Throwable ex) {
                Throwable target = unwrap(ex);
                FeignLogSupport.log(logger, LogLevel.ERROR, "{}<!! failed: {}", prefix, target.getMessage(), target);
                throw target;
            }
        });
        return proxyFactory.getProxy();
    }

    private Throwable unwrap(Throwable ex) {
        if (ex instanceof InvocationTargetException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }
}
