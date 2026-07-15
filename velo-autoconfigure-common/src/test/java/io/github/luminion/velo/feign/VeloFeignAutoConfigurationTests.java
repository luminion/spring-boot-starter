package io.github.luminion.velo.feign;

import feign.RequestTemplate;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogSource;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.trace.TraceContext;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VeloFeignAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AopAutoConfiguration.class,
                    VeloCoreAutoConfiguration.class,
                    VeloFeignAutoConfiguration.class
            ))
            .withUserConfiguration(TestFeignConfiguration.class);

    @Test
    void shouldCreateFeignLogAspectWhenFeignClientPresent() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(FeignLogAspect.class));
    }

    @Test
    void shouldCreateFeignLogAspectWithFallbackWriterWhenInvocationLogWriterMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        VeloCoreAutoConfiguration.class,
                        VeloFeignAutoConfiguration.class
                ))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(InvocationLogWriter.class);
                    assertThat(context).hasSingleBean(FeignLogAspect.class);
                });
    }

    @Test
    void shouldSkipFeignLogAspectWhenFeignInvocationLoggingDisabled() {
        contextRunner
                .withPropertyValues("velo.log.invocation.feign.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FeignLogAspect.class));
    }

    @Test
    void shouldSkipFeignLogAspectWhenInvocationLoggingDisabled() {
        contextRunner
                .withPropertyValues("velo.log.invocation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FeignLogAspect.class));
    }

    @Test
    void shouldSkipFeignLogBeansWhenLogDisabled() {
        contextRunner
                .withPropertyValues("velo.log.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(FeignLogAspect.class);
                    assertThat(context).doesNotHaveBean(FeignTraceRequestInterceptor.class);
                });
    }

    @Test
    void shouldSkipAutoConfigurationWhenFeignClientClassMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.cloud.openfeign"))
                .run(context -> assertThat(context).doesNotHaveBean(FeignLogAspect.class));
    }

    @Test
    void shouldLogFeignInvocationWithArgumentsAndResponse() throws Throwable {
        CapturingInvocationLogWriter writer = invokeFindById(new VeloProperties(), runtimeJsonSerializer());

        // 进入记录（index 0）：含入参，不含返回值
        assertThat(writer.records).hasSize(2);
        InvocationLogRecord entry = writer.records.get(0);
        assertThat(entry.getPhase()).isEqualTo(InvocationPhase.ENTRY);
        assertThat(entry.getSource()).isEqualTo(InvocationLogSource.FEIGN);
        assertThat(entry.getTarget()).isEqualTo("GET /users/{id}");
        assertThat(entry.isSuccess()).isTrue();
        assertThat(entry.getArgs()).contains("\"id\":1");

        // 退出记录（index 1）：含耗时与返回值
        InvocationLogRecord exit = writer.records.get(1);
        assertThat(exit.getPhase()).isEqualTo(InvocationPhase.EXIT);
        assertThat(exit.isSuccess()).isTrue();
        assertThat(exit.getResult()).isEqualTo("{\"userName\":\"tom\"}");
    }

    @Test
    void shouldTruncatePayloadsUsingConfiguredLimit() throws Throwable {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getInvocation().setMaxPayloadLength(13);

        CapturingInvocationLogWriter writer = invokeFindById(properties, runtimeJsonSerializer());

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(1).getResult()).isEqualTo("{\"userName...");
    }

    @Test
    void shouldAllowUnlimitedPayloadLoggingWhenConfiguredAsNegativeOne() throws Throwable {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getInvocation().setMaxPayloadLength(-1);

        CapturingInvocationLogWriter writer = invokeFindById(properties, runtimeJsonSerializer());

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(1).getResult()).isEqualTo("{\"userName\":\"tom\"}");
    }

    @Test
    void shouldLogDashWhenPayloadLimitIsZero() throws Throwable {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getInvocation().setMaxPayloadLength(0);

        CapturingInvocationLogWriter writer = invokeFindById(properties, runtimeJsonSerializer());

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(0).getArgs()).isEqualTo("-");   // ENTRY
        assertThat(writer.records.get(1).getResult()).isEqualTo("-"); // EXIT
    }

    @Test
    void shouldLogDashWhenResponseBodyIsNull() throws Throwable {
        CapturingInvocationLogWriter writer = invokePing();

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(1).getResult()).isEqualTo("-"); // EXIT
    }

    @Test
    void shouldLogFailuresWithoutSwallowingException() throws Throwable {
        CapturingInvocationLogWriter writer = invokeFail();

        assertThat(writer.records).hasSize(2);
        // ENTRY 记录：入参已写入，方法尚未执行
        assertThat(writer.records.get(0).getPhase()).isEqualTo(InvocationPhase.ENTRY);
        assertThat(writer.records.get(0).isSuccess()).isTrue();
        // EXIT 记录：包含异常信息
        InvocationLogRecord exit = writer.records.get(1);
        assertThat(exit.getPhase()).isEqualTo(InvocationPhase.EXIT);
        assertThat(exit.isSuccess()).isFalse();
        assertThat(exit.getTarget()).isEqualTo("GET /users/fail");
        assertThat(exit.getError()).isInstanceOf(IllegalStateException.class);
        assertThat(exit.getErrorMessage()).isEqualTo("boom");
    }

    @Test
    void shouldKeepSuccessfulInvocationWhenPayloadSerializationFails() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        RuntimeJsonSerializer serializer = value -> {
            throw new IllegalStateException("serialization failed");
        };
        FeignLogAspect aspect = new FeignLogAspect(properties, serializer, writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        ResponseEntity<DemoPayload> expected = ResponseEntity.ok(new DemoPayload("tom"));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenReturn(expected);
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("findById", Long.class));
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});

        assertThat(aspect.logFeignInvocation(joinPoint)).isSameAs(expected);
        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(0).getArgs()).isEqualTo("-");   // ENTRY：序列化失败降级为 -
        assertThat(writer.records.get(1).getResult()).isEqualTo("-"); // EXIT：序列化失败降级为 -
    }

    @Test
    void shouldKeepSuccessfulInvocationWhenWriterFails() throws Throwable {
        VeloProperties properties = new VeloProperties();
        InvocationLogWriter writer = record -> {
            throw new IllegalStateException("writer failed");
        };
        FeignLogAspect aspect = new FeignLogAspect(properties, runtimeJsonSerializer(), writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        ResponseEntity<DemoPayload> expected = ResponseEntity.ok(new DemoPayload("tom"));

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenReturn(expected);
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("findById", Long.class));
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});

        assertThat(aspect.logFeignInvocation(joinPoint)).isSameAs(expected);
    }

    @Test
    void shouldPreserveBusinessExceptionWhenWriterFails() throws Throwable {
        VeloProperties properties = new VeloProperties();
        InvocationLogWriter writer = record -> {
            throw new IllegalStateException("writer failed");
        };
        FeignLogAspect aspect = new FeignLogAspect(properties, runtimeJsonSerializer(), writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        IllegalArgumentException businessError = new IllegalArgumentException("business failed");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(businessError);
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("fail"));
        when(signature.getParameterNames()).thenReturn(new String[0]);

        assertThatThrownBy(() -> aspect.logFeignInvocation(joinPoint)).isSameAs(businessError);
    }

    @Test
    void shouldPreserveBusinessExceptionWhenArgumentSerializationFails() throws Throwable {
        VeloProperties properties = new VeloProperties();
        RuntimeJsonSerializer serializer = value -> {
            throw new IllegalStateException("serialization failed");
        };
        FeignLogAspect aspect = new FeignLogAspect(properties, serializer, new CapturingInvocationLogWriter());
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        IllegalArgumentException businessError = new IllegalArgumentException("business failed");

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenThrow(businessError);
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("findById", Long.class));
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});

        assertThatThrownBy(() -> aspect.logFeignInvocation(joinPoint)).isSameAs(businessError);
    }

    @Test
    void shouldNotSwallowWriterError() throws Throwable {
        VeloProperties properties = new VeloProperties();
        AssertionError writerError = new AssertionError("writer error");
        InvocationLogWriter writer = record -> {
            throw writerError;
        };
        FeignLogAspect aspect = new FeignLogAspect(properties, runtimeJsonSerializer(), writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new DemoPayload("tom")));
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("findById", Long.class));
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});

        assertThatThrownBy(() -> aspect.logFeignInvocation(joinPoint)).isSameAs(writerError);
    }

    @Test
    void shouldPropagateTraceIdToFeignRequestTemplate() {
        VeloProperties properties = new VeloProperties();
        FeignTraceRequestInterceptor interceptor = new FeignTraceRequestInterceptor(properties);
        RequestTemplate requestTemplate = new RequestTemplate();
        TraceContext.put(properties.getLog().getTrace().getMdcKey(), "trace-001");
        try {
            interceptor.apply(requestTemplate);
        } finally {
            TraceContext.remove(properties.getLog().getTrace().getMdcKey());
        }

        Collection<String> values = requestTemplate.headers().get("X-Trace-Id");
        assertThat(values).containsExactly("trace-001");
    }

    private CapturingInvocationLogWriter invokeFindById(VeloProperties properties, RuntimeJsonSerializer serializer)
            throws Throwable {
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        FeignLogAspect aspect = new FeignLogAspect(properties, serializer, writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[] {1L});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(new DemoPayload("tom")));
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("findById", Long.class));
        when(signature.getParameterNames()).thenReturn(new String[] {"id"});

        aspect.logFeignInvocation(joinPoint);
        return writer;
    }

    private CapturingInvocationLogWriter invokePing() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        FeignLogAspect aspect = new FeignLogAspect(properties, value -> "null", writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn((NullBodyFeignClient) () -> null);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(null);
        when(signature.getMethod()).thenReturn(NullBodyFeignClient.class.getDeclaredMethod("ping"));
        when(signature.getParameterNames()).thenReturn(new String[0]);

        aspect.logFeignInvocation(joinPoint);
        return writer;
    }

    private CapturingInvocationLogWriter invokeFail() throws Throwable {
        VeloProperties properties = new VeloProperties();
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        FeignLogAspect aspect = new FeignLogAspect(properties, runtimeJsonSerializer(), writer);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new DemoFeignClientImpl());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        when(signature.getMethod()).thenReturn(DemoFeignClient.class.getDeclaredMethod("fail"));
        when(signature.getParameterNames()).thenReturn(new String[0]);

        try {
            aspect.logFeignInvocation(joinPoint);
        } catch (IllegalStateException ignored) {
        }
        return writer;
    }

    private RuntimeJsonSerializer runtimeJsonSerializer() {
        return value -> {
            if (value instanceof DemoPayload) {
                DemoPayload payload = (DemoPayload) value;
                return "{\"userName\":\"" + payload.getUserName() + "\"}";
            }
            if (value instanceof Long) {
                return String.valueOf(value);
            }
            if (value instanceof java.util.Map<?, ?>) {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
                if (map.containsKey("id")) {
                    return "{\"id\":" + map.get("id") + "}";
                }
                return "{\"value\":\"abcdefghijklmnopqrstuvwxyz\"}";
            }
            return "null";
        };
    }

    @Configuration(proxyBeanMethods = false)
    static class TestFeignConfiguration {

        @Bean
        RuntimeJsonSerializer runtimeJsonSerializer() {
            return value -> {
                if (value instanceof DemoPayload) {
                    DemoPayload payload = (DemoPayload) value;
                    return "{\"userName\":\"" + payload.getUserName() + "\"}";
                }
                if (value instanceof Long) {
                    return String.valueOf(value);
                }
                if (value instanceof java.util.Map<?, ?>) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
                    if (map.containsKey("id")) {
                        return "{\"id\":" + map.get("id") + "}";
                    }
                    return "{\"value\":\"abcdefghijklmnopqrstuvwxyz\"}";
                }
                return "null";
            };
        }

        @Bean
        InvocationLogWriter invocationLogWriter() {
            return new CapturingInvocationLogWriter();
        }

        @Bean
        DemoFeignClient demoFeignClient() {
            return new DemoFeignClientImpl();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NullBodyFeignConfiguration {

        @Bean
        RuntimeJsonSerializer runtimeJsonSerializer() {
            return value -> "null";
        }

        @Bean
        InvocationLogWriter invocationLogWriter() {
            return new CapturingInvocationLogWriter();
        }

        @Bean
        NullBodyFeignClient nullBodyFeignClient() {
            return () -> null;
        }
    }

    @FeignClient(name = "demo-client")
    @RequestMapping("/users")
    interface DemoFeignClient {

        @GetMapping("/{id}")
        ResponseEntity<DemoPayload> findById(@PathVariable("id") Long id);

        @GetMapping("/fail")
        ResponseEntity<DemoPayload> fail();
    }

    static class DemoFeignClientImpl implements DemoFeignClient {

        @Override
        public ResponseEntity<DemoPayload> findById(Long id) {
            return ResponseEntity.ok(new DemoPayload("tom"));
        }

        @Override
        public ResponseEntity<DemoPayload> fail() {
            throw new IllegalStateException("boom");
        }
    }

    @FeignClient(name = "null-body-client")
    interface NullBodyFeignClient {

        @GetMapping("/ping")
        ResponseEntity<Void> ping();
    }

    static class DemoPayload {

        private final String userName;

        DemoPayload(String userName) {
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }
    }

    static final class CapturingInvocationLogWriter implements InvocationLogWriter {

        private final List<InvocationLogRecord> records = new ArrayList<>();

        @Override
        public void write(InvocationLogRecord record) {
            records.add(record);
        }
    }
}
