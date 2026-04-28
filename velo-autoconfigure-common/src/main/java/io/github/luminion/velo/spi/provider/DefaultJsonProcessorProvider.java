package io.github.luminion.velo.spi.provider;

import io.github.luminion.velo.spi.JsonProcessorProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Constructor;
import java.util.function.Function;

/**
 * @author luminion
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class DefaultJsonProcessorProvider implements JsonProcessorProvider {
    private final BeanFactory beanFactory;

    @Override
    public Function<String, String> getProcessor(Class<? extends Function<String, String>> clazz) {
        ObjectProvider<? extends Function<String, String>> beanProvider = beanFactory.getBeanProvider(clazz);
        Function<String, String> bean = beanProvider.getIfAvailable();
        if (bean != null) {
            return bean;
        }
        try {
            Constructor<? extends Function<String, String>> declaredConstructor = clazz.getDeclaredConstructor();
            return declaredConstructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("指定的处理器类" + clazz + "实例化失败,请提供无参构造或将其注入到Spring容器", e);
        }
    }


}
