package io.github.luminion.velo.spi.provider;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredHttpMessageConverterRuntimeJsonSerializerTest {

    @Test
    void shouldResolveConvertersAfterSingletonInitialization() {
        AtomicInteger supplierCalls = new AtomicInteger();
        DeferredHttpMessageConverterRuntimeJsonSerializer serializer =
                new DeferredHttpMessageConverterRuntimeJsonSerializer(() -> {
                    supplierCalls.incrementAndGet();
                    return Collections.singletonList(new MarkerJsonConverter());
                });

        assertThat(supplierCalls).hasValue(0);
        assertThat(serializer.toJson(Collections.singletonMap("value", "before")))
                .doesNotContain("resolved");
        assertThat(supplierCalls).hasValue(0);

        serializer.afterSingletonsInstantiated();

        assertThat(supplierCalls).hasValue(1);
        assertThat(serializer.toJson(Collections.singletonMap("value", "after")))
                .isEqualTo("{\"resolved\":true}");
    }

    private static final class MarkerJsonConverter implements HttpMessageConverter<Object> {

        @Override
        public boolean canRead(Class<?> clazz, MediaType mediaType) {
            return false;
        }

        @Override
        public boolean canWrite(Class<?> clazz, MediaType mediaType) {
            return mediaType != null && MediaType.APPLICATION_JSON.isCompatibleWith(mediaType);
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return Collections.singletonList(MediaType.APPLICATION_JSON);
        }

        @Override
        public Object read(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
            return null;
        }

        @Override
        public void write(Object value, MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
            outputMessage.getBody().write("{\"resolved\":true}".getBytes(StandardCharsets.UTF_8));
        }
    }
}
