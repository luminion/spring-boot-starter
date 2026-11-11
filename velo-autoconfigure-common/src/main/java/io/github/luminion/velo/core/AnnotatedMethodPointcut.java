package io.github.luminion.velo.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ReflectionUtils;

/**
 * 按注解匹配方法的静态切点；不扫描 Object 方法、私有方法和静态方法，注解按类名匹配，可选依赖中的注解无需提前加载。
 * <p>
 * 两种查找范围：
 * <ul>
 *     <li>层级查找（{@code forMethods} 等）：支持组合注解及接口、父类上的声明；</li>
 *     <li>直接声明（{@code forDeclaredMethods} 等）：只认实现方法及其声明类上直接标注的注解（含组合注解），
 *     不搜索父接口、父类，与 AspectJ 的 {@code @annotation} / {@code @within} 语义一致。</li>
 * </ul>
 *
 * @author luminion
 */
public final class AnnotatedMethodPointcut extends StaticMethodMatcherPointcut {
    private final List<String> annotationNames;
    private final boolean typeLevel;
    private final boolean publicOnly;
    private final boolean declaredOnly;

    private AnnotatedMethodPointcut(List<String> annotationNames, boolean typeLevel, boolean publicOnly,
            boolean declaredOnly) {
        this.annotationNames = Collections.unmodifiableList(new ArrayList<>(annotationNames));
        this.typeLevel = typeLevel;
        this.publicOnly = publicOnly;
        this.declaredOnly = declaredOnly;
        setClassFilter(new CandidateClassFilter());
    }

    /**
     * 只匹配方法上的注解。
     *
     * @param annotationTypes 注解类型
     * @return 方法注解切点
     */
    @SafeVarargs
    public static AnnotatedMethodPointcut forMethods(Class<? extends Annotation>... annotationTypes) {
        return new AnnotatedMethodPointcut(names(annotationTypes), false, false, false);
    }

    /**
     * 只匹配方法上的注解，注解按类名指定。
     *
     * @param annotationNames 注解类名
     * @return 方法注解切点
     */
    public static AnnotatedMethodPointcut forMethods(String... annotationNames) {
        return new AnnotatedMethodPointcut(Arrays.asList(annotationNames), false, false, false);
    }

    /**
     * 匹配方法或类型上的注解；类型上的注解作用于该类型的全部可代理方法。
     *
     * @param annotationTypes 注解类型
     * @return 方法或类型注解切点
     */
    @SafeVarargs
    public static AnnotatedMethodPointcut forMethodsOrTypes(Class<? extends Annotation>... annotationTypes) {
        return new AnnotatedMethodPointcut(names(annotationTypes), true, false, false);
    }

    /**
     * 只匹配 public 方法，方法或类型上带注解即可。
     *
     * @param annotationTypes 注解类型
     * @return public 方法切点
     */
    @SafeVarargs
    public static AnnotatedMethodPointcut forPublicMethodsOrTypes(Class<? extends Annotation>... annotationTypes) {
        return new AnnotatedMethodPointcut(names(annotationTypes), true, true, false);
    }

    /**
     * 只匹配实现方法上直接声明的注解，不搜索父接口、父类。
     *
     * @param annotationTypes 注解类型
     * @return 直接声明切点
     */
    @SafeVarargs
    public static AnnotatedMethodPointcut forDeclaredMethods(Class<? extends Annotation>... annotationTypes) {
        return new AnnotatedMethodPointcut(names(annotationTypes), false, false, true);
    }

    /**
     * 匹配实现方法上直接声明的注解；方法上没有时，匹配声明该方法的类型上直接声明的注解。
     *
     * @param annotationTypes 注解类型
     * @return 直接声明切点
     */
    @SafeVarargs
    public static AnnotatedMethodPointcut forDeclaredMethodsOrTypes(Class<? extends Annotation>... annotationTypes) {
        return new AnnotatedMethodPointcut(names(annotationTypes), true, false, true);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (!isInterceptable(method, publicOnly)) {
            return false;
        }
        Class<?> type = targetClass == null ? method.getDeclaringClass() : targetClass;
        Method specific = AopUtils.getMostSpecificMethod(method, type);
        if (declaredOnly) {
            return present(specific) || (typeLevel && present(specific.getDeclaringClass()));
        }
        if (present(specific) || (specific != method && present(method))) {
            return true;
        }
        return typeLevel && (present(type) || present(method.getDeclaringClass()));
    }

    /**
     * 判断方法能否被代理拦截：排除 Object 方法、私有方法和静态方法。
     *
     * @param method     候选方法
     * @param publicOnly 是否只接受 public 方法
     * @return 能被拦截时返回 {@code true}
     */
    public static boolean isInterceptable(Method method, boolean publicOnly) {
        int modifiers = method.getModifiers();
        if (Modifier.isPrivate(modifiers) || Modifier.isStatic(modifiers) || ReflectionUtils.isObjectMethod(method)) {
            return false;
        }
        return !publicOnly || Modifier.isPublic(modifiers);
    }

    private boolean present(AnnotatedElement element) {
        MergedAnnotations annotations = MergedAnnotations.from(element, declaredOnly
                ? MergedAnnotations.SearchStrategy.DIRECT : MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        for (String name : annotationNames) {
            if (annotations.isPresent(name)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> names(Class<? extends Annotation>[] annotationTypes) {
        List<String> names = new ArrayList<>();
        for (Class<? extends Annotation> type : annotationTypes) {
            names.add(type.getName());
        }
        return names;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotatedMethodPointcut)) {
            return false;
        }
        AnnotatedMethodPointcut that = (AnnotatedMethodPointcut) other;
        return typeLevel == that.typeLevel && publicOnly == that.publicOnly && declaredOnly == that.declaredOnly
                && annotationNames.equals(that.annotationNames);
    }

    @Override
    public int hashCode() {
        return annotationNames.hashCode() * 31 + (declaredOnly ? 4 : 0) + (typeLevel ? 2 : 0) + (publicOnly ? 1 : 0);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + annotationNames;
    }

    /**
     * JDK 自带类型不会携带 Starter 注解，直接跳过。
     */
    private static final class CandidateClassFilter implements ClassFilter {
        @Override
        public boolean matches(Class<?> clazz) {
            return !clazz.getName().startsWith("java.");
        }
    }
}
