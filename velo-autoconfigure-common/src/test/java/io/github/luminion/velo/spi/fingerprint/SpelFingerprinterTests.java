package io.github.luminion.velo.spi.fingerprint;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpelFingerprinterTests {

    private final SpelFingerprinter fingerprinter = new SpelFingerprinter();
    private final Method method = SampleService.class.getDeclaredMethod("execute", Object.class);
    private final String methodKey = SampleService.class.getName() + "#execute(java.lang.Object)";

    SpelFingerprinterTests() throws NoSuchMethodException {
    }

    @Test
    void shouldScopeExpressionByFullMethodAndPreserveWhitespace() {
        assertThat(resolve("  id  ", "#p0")).isEqualTo(methodKey + ":  id  ");
        assertThat(resolve("id", "#p0")).isEqualTo(methodKey + ":id");
        assertThat(resolve("id", "")).isEqualTo(methodKey);
    }

    @Test
    void shouldDistinguishOverloadsAndActualTargetClasses() throws NoSuchMethodException {
        Method stringMethod = OverloadedService.class.getDeclaredMethod("execute", String.class);
        Method longMethod = OverloadedService.class.getDeclaredMethod("execute", Long.class);
        assertThat(fingerprinter.resolveMethodFingerprint(new OverloadedService(), stringMethod,
                new Object[]{"id"}, "")).isEqualTo(OverloadedService.class.getName() + "#execute(java.lang.String)");
        assertThat(fingerprinter.resolveMethodFingerprint(new OverloadedService(), longMethod,
                new Object[]{1L}, "")).isEqualTo(OverloadedService.class.getName() + "#execute(java.lang.Long)");
        assertThat(fingerprinter.resolveMethodFingerprint(new FirstService(), method, new Object[]{"id"}, "#p0"))
                .isEqualTo(FirstService.class.getName() + "#execute(java.lang.Object):id");
        assertThat(fingerprinter.resolveMethodFingerprint(new SecondService(), method, new Object[]{"id"}, "#p0"))
                .isEqualTo(SecondService.class.getName() + "#execute(java.lang.Object):id");
    }

    @Test
    void shouldUseUserClassForProxyTarget() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleService());
        factory.setProxyTargetClass(true);
        Object proxy = factory.getProxy();
        assertThat(fingerprinter.resolveMethodFingerprint(proxy, method, new Object[]{"id"}, "#p0"))
                .isEqualTo(methodKey + ":id");
    }

    @Test
    void shouldRejectNullBlankAndUnstableObjectResults() {
        assertThatThrownBy(() -> resolve(null, "#p0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("resolved to null");
        assertThatThrownBy(() -> resolve("   ", "#p0"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank value");
        for (Object value : new Object[]{new Object(), new String[]{"id"}, Collections.singletonMap("id", 1)}) {
            assertThatThrownBy(() -> resolve(value, "#p0"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("scalar value");
        }
    }

    @Test
    void shouldSupportScalarValuesAndStableEnumName() {
        UUID uuid = UUID.randomUUID();
        for (Object value : new Object[]{123L, true, 'A', uuid}) {
            assertThat(resolve(value, "#p0")).isEqualTo(methodKey + ':' + value);
        }
        assertThat(resolve(Status.READY, "#p0")).isEqualTo(methodKey + ":READY");
    }

    @Test
    void shouldReuseDeclaredExpressionsWithoutClearingCache() {
        CountingParser parser = new CountingParser();
        SpelFingerprinter cached = new SpelFingerprinter(parser);
        for (int i = 0; i < 300; i++) {
            cached.resolveMethodFingerprint(new SampleService(), method, new Object[]{"id"}, "#p0 + ':' + " + i);
        }
        assertThat(cached.resolveMethodFingerprint(new SampleService(), method,
                new Object[]{"next"}, "#p0 + ':' + 0")).isEqualTo(methodKey + ":next:0");
        assertThat(parser.parses.get()).isEqualTo(300);
    }

    @Test
    void shouldResolveNamedParametersAndArgumentProperties() {
        assertThat(resolve(new KeyArgument("order-1"), "#value.id"))
                .isEqualTo(methodKey + ":order-1");
        assertThat(resolve(new KeyArgument("order-2"), "#value.id"))
                .isEqualTo(methodKey + ":order-2");
    }

    @Test
    void shouldIsolateCachedExpressionMetadataByActualUserClass() {
        CountingParser parser = new CountingParser();
        SpelFingerprinter cached = new SpelFingerprinter(parser);
        assertThat(cached.resolveMethodFingerprint(new FirstService(), method,
                new Object[]{new KeyArgument("first")}, "#value.id"))
                .isEqualTo(FirstService.class.getName() + "#execute(java.lang.Object):first");
        assertThat(cached.resolveMethodFingerprint(new SecondService(), method,
                new Object[]{new KeyArgument("second")}, "#value.id"))
                .isEqualTo(SecondService.class.getName() + "#execute(java.lang.Object):second");
        cached.resolveMethodFingerprint(new FirstService(), method,
                new Object[]{new KeyArgument("next")}, "#value.id");

        assertThat(parser.parses.get()).isEqualTo(2);
    }

    @Test
    void concurrentEvaluationShouldKeepRequestArgumentsIsolated() throws Exception {
        CountingParser parser = new CountingParser();
        SpelFingerprinter cached = new SpelFingerprinter(parser);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final int index = i;
                results.add(executor.submit(() -> cached.resolveMethodFingerprint(new SampleService(), method,
                        new Object[]{"id-" + index}, "#p0")));
            }
            for (int i = 0; i < results.size(); i++) {
                assertThat(results.get(i).get()).isEqualTo(methodKey + ":id-" + i);
            }
            assertThat(parser.parses.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private String resolve(Object value, String expression) {
        return fingerprinter.resolveMethodFingerprint(new SampleService(), method, new Object[]{value}, expression);
    }

    private static final class CountingParser implements ExpressionParser {
        private final AtomicInteger parses = new AtomicInteger();
        private final ExpressionParser delegate = new SpelExpressionParser();

        @Override
        public Expression parseExpression(String expression) {
            parses.incrementAndGet();
            return delegate.parseExpression(expression);
        }

        @Override
        public Expression parseExpression(String expression, ParserContext context) {
            parses.incrementAndGet();
            return delegate.parseExpression(expression, context);
        }
    }

    enum Status {
        READY;

        @Override
        public String toString() {
            return "可变显示名称";
        }
    }

    static class SampleService {
        public void execute(Object value) {
        }
    }

    static class KeyArgument {
        private final String id;

        KeyArgument(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    static class FirstService extends SampleService {
    }

    static class SecondService extends SampleService {
    }

    static class OverloadedService {
        void execute(String value) {
        }

        void execute(Long value) {
        }
    }
}
