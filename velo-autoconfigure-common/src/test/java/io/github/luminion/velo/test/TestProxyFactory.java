package io.github.luminion.velo.test;

import java.lang.reflect.Proxy;

public final class TestProxyFactory {

    private TestProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> null
        );
    }
}
