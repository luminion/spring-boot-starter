package io.github.luminion.starter.core.spi;

import java.util.function.Function;

/**
 * JSON 处理器实例提供者
 *
 * @author luminion
 * @since 1.0.0
 */
@FunctionalInterface
public interface JsonProcessorProvider {

    /**
     * 根据指定的 Class 获取处理器实例
     */
    Function<String, String> getProcessor(Class<? extends Function<String, String>> clazz);

}