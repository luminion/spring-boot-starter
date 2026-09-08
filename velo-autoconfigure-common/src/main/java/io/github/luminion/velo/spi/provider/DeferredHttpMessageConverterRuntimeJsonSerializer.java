package io.github.luminion.velo.spi.provider;

import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 延迟绑定 MVC 消息转换器的运行时 JSON 序列化器。
 *
 * <p>自动配置创建 Bean 时，MVC 的 {@code RequestMappingHandlerAdapter} 可能尚未完成初始化。
 * 通过 {@link SmartInitializingSingleton} 延后读取转换器列表，确保常规单例初始化完成后使用应用实际配置的转换器。</p>
 */
public class DeferredHttpMessageConverterRuntimeJsonSerializer
        implements RuntimeJsonSerializer, SmartInitializingSingleton {

    private final Supplier<List<HttpMessageConverter<?>>> converterSupplier;

    private volatile RuntimeJsonSerializer delegate =
            new HttpMessageConverterRuntimeJsonSerializer(Collections.emptyList());

    public DeferredHttpMessageConverterRuntimeJsonSerializer(
            Supplier<List<HttpMessageConverter<?>>> converterSupplier) {
        this.converterSupplier = Objects.requireNonNull(converterSupplier, "converterSupplier");
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<HttpMessageConverter<?>> converters = converterSupplier.get();
        if (converters == null) {
            converters = Collections.emptyList();
        }
        delegate = new HttpMessageConverterRuntimeJsonSerializer(converters);
    }

    @Override
    public String toJson(Object value) {
        return delegate.toJson(value);
    }
}
