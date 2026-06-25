package io.github.luminion.velo.core;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class VeloMessageResolverTest {

    @Test
    void shouldReturnPlainTextAsIsWhenNoMessageSource() {
        VeloMessageResolver resolver = new VeloMessageResolver();
        assertThat(resolver.resolve("您的请求已提交，请勿重复操作"))
                .isEqualTo("您的请求已提交，请勿重复操作");
    }

    @Test
    void shouldReturnPlainTextAsIsEvenWithMessageSource() {
        VeloMessageResolver resolver = new VeloMessageResolver();
        resolver.setMessageSource(new StaticMessageSource());
        assertThat(resolver.resolve("普通文本")).isEqualTo("普通文本");
    }

    @Test
    void shouldFallBackToKeyWhenNoMessageSource() {
        VeloMessageResolver resolver = new VeloMessageResolver();
        assertThat(resolver.resolve("{velo.idempotent.rejected}"))
                .isEqualTo("velo.idempotent.rejected");
    }

    @Test
    void shouldResolveKeyFromMessageSource() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("velo.idempotent.rejected", Locale.getDefault(), "请勿重复提交");
        VeloMessageResolver resolver = new VeloMessageResolver();
        resolver.setMessageSource(source);

        assertThat(resolver.resolve("{velo.idempotent.rejected}")).isEqualTo("请勿重复提交");
    }

    @Test
    void shouldFallBackToKeyWhenMessageMissing() {
        VeloMessageResolver resolver = new VeloMessageResolver();
        resolver.setMessageSource(new StaticMessageSource());
        assertThat(resolver.resolve("{velo.unknown.key}")).isEqualTo("velo.unknown.key");
    }

    @Test
    void shouldNotTreatPartialBracesAsKey() {
        VeloMessageResolver resolver = new VeloMessageResolver();
        resolver.setMessageSource(new StaticMessageSource());
        assertThat(resolver.resolve("{not a key")).isEqualTo("{not a key");
        assertThat(resolver.resolve("prefix {x}")).isEqualTo("prefix {x}");
    }

    @Test
    void shouldReturnNullForNull() {
        VeloMessageResolver resolver = new VeloMessageResolver();
        assertThat(resolver.resolve(null)).isNull();
    }
}
