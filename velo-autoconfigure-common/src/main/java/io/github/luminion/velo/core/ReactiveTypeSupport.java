package io.github.luminion.velo.core;

/**
 * 响应式返回值判断工具。
 *
 * <p>该类不直接引用 Reactor，保证同步自动配置在未引入 WebFlux 时仍可安全加载。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
public final class ReactiveTypeSupport {

    private static final String PUBLISHER_CLASS_NAME = "org.reactivestreams.Publisher";

    private ReactiveTypeSupport() {
    }

    /**
     * 判断返回类型是否为 Reactive Streams Publisher。
     *
     * @param returnType 方法返回类型
     * @return 是 Publisher 返回类型时返回 {@code true}
     */
    public static boolean isReactiveType(Class<?> returnType) {
        if (returnType == null) {
            return false;
        }
        ClassLoader classLoader = returnType.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            classLoader = ReactiveTypeSupport.class.getClassLoader();
        }
        try {
            Class<?> publisherType = Class.forName(PUBLISHER_CLASS_NAME, false, classLoader);
            return publisherType.isAssignableFrom(returnType);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
}
