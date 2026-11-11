package io.github.luminion.velo.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 按调用方法解析功能注解并缓存；实现方法上的声明优先，其次是声明该方法的类型上的声明。
 * <p>
 * 查找语义与 {@link io.github.luminion.velo.core.AnnotatedMethodPointcut#forDeclaredMethodsOrTypes} 一致：
 * 只认直接声明（含组合注解），不搜索父接口、父类，并处理 {@code @AliasFor}。缓存随目标类释放，只保存实际调用过的方法。
 *
 * @param <A> 注解类型
 * @author luminion
 */
public final class InvocationAnnotationResolver<A extends Annotation> {
    private final Class<A> type;
    private final boolean typeLevel;
    private final ClassValue<ConcurrentMap<Method, A>> cache = new ClassValue<ConcurrentMap<Method, A>>() {
        @Override
        protected ConcurrentMap<Method, A> computeValue(Class<?> targetClass) {
            return new ConcurrentHashMap<>();
        }
    };

    /**
     * 创建注解解析器。
     *
     * @param type      注解类型
     * @param typeLevel 方法上没有声明时，是否继续查找类型上的声明
     */
    public InvocationAnnotationResolver(Class<A> type, boolean typeLevel) {
        this.type = Objects.requireNonNull(type, "type");
        this.typeLevel = typeLevel;
    }

    /**
     * 解析本次调用生效的注解。
     *
     * @param method 被调用的方法
     * @param target 调用目标，可为 {@code null}
     * @return 生效的注解；没有声明时返回 {@code null}
     */
    public A resolve(Method method, Object target) {
        Class<?> targetClass = target == null ? method.getDeclaringClass() : AopUtils.getTargetClass(target);
        ConcurrentMap<Method, A> methods = cache.get(targetClass);
        A cached = methods.get(method);
        if (cached != null) {
            return cached;
        }
        Method specific = AopUtils.getMostSpecificMethod(method, targetClass);
        A found = AnnotatedElementUtils.getMergedAnnotation(specific, type);
        if (found == null && typeLevel) {
            found = AnnotatedElementUtils.getMergedAnnotation(specific.getDeclaringClass(), type);
        }
        if (found != null) {
            methods.putIfAbsent(method, found);
        }
        return found;
    }
}
