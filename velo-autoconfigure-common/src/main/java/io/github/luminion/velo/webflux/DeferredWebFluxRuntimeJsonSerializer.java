package io.github.luminion.velo.webflux;

import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.codec.HttpMessageWriter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 延迟绑定 WebFlux 消息写入器的运行时 JSON 序列化器。
 *
 * <p>WebFlux 编解码器可能在 Velo 自动配置创建日志切面之后才完成初始化，因此在所有单例
 * 初始化完成后再读取实际写入器，避免提前创建空序列化器。</p>
 *
 * @author luminion
 * @since 1.3.1
 */
final class DeferredWebFluxRuntimeJsonSerializer implements RuntimeJsonSerializer, SmartInitializingSingleton {

    private final Supplier<List<HttpMessageWriter<?>>> writerSupplier;

    private volatile RuntimeJsonSerializer delegate =
            new WebFluxRuntimeJsonSerializer(Collections.emptyList());

    DeferredWebFluxRuntimeJsonSerializer(Supplier<List<HttpMessageWriter<?>>> writerSupplier) {
        this.writerSupplier = Objects.requireNonNull(writerSupplier, "writerSupplier");
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<HttpMessageWriter<?>> writers = writerSupplier.get();
        delegate = new WebFluxRuntimeJsonSerializer(writers == null ? Collections.emptyList() : writers);
    }

    @Override
    public String toJson(Object value) {
        return delegate.toJson(value);
    }
}
