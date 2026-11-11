package io.github.luminion.velo.core;

import java.util.Objects;
import java.util.function.Supplier;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.Ordered;
import org.springframework.util.function.SingletonSupplier;

/**
 * Starter 内置 Advisor：创建时只确定切点和顺序，拦截器 Bean 在容器单例初始化完成后再解析。
 * <p>
 * Spring 注册 BeanPostProcessor 时会提前实例化全部 Advisor，并在排序时读取顺序。本类的切点和顺序都不依赖其他 Bean，
 * 拦截器及其依赖（配置属性、ObjectMapper 等）因此不会被提前创建，仍能被所有后置处理器正常处理。
 *
 * @author luminion
 */
public class DeferredInterceptorAdvisor implements PointcutAdvisor, Ordered, SmartInitializingSingleton {
    private final Pointcut pointcut;
    private final int order;
    private final SingletonSupplier<MethodInterceptor> interceptor;
    private final MethodInterceptor advice;

    /**
     * 创建延迟解析拦截器的 Advisor。
     *
     * @param pointcut    静态切点，不得依赖其他 Bean
     * @param order       Advisor 执行顺序
     * @param interceptor 拦截器提供器，首次调用或单例初始化完成后解析一次
     */
    public DeferredInterceptorAdvisor(Pointcut pointcut, int order, Supplier<? extends MethodInterceptor> interceptor) {
        this.pointcut = Objects.requireNonNull(pointcut, "pointcut");
        this.order = order;
        Objects.requireNonNull(interceptor, "interceptor");
        this.interceptor = SingletonSupplier.of(() -> interceptor.get());
        this.advice = (MethodInterceptor) invocation -> this.interceptor.obtain().invoke(invocation);
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    @Override
    public boolean isPerInstance() {
        return true;
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * 所有单例创建完成后预先解析拦截器，使缺失或不唯一的拦截器在启动期报错。
     */
    @Override
    public void afterSingletonsInstantiated() {
        interceptor.obtain();
    }
}
