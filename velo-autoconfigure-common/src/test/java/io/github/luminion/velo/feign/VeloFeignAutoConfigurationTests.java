package io.github.luminion.velo.feign;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
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
    void shouldSkipFeignLogAspectWhenRequestLoggingDisabled() {
        contextRunner
                .withPropertyValues("velo.feign.request-logging-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FeignLogAspect.class));
    }

    @Test
    void shouldSkipAutoConfigurationWhenFeignClientClassMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.cloud.openfeign"))
                .run(context -> assertThat(context).doesNotHaveBean(FeignLogAspect.class));
    }

    @Test
    void shouldLogFeignInvocationWithArgumentsAndResponse(CapturedOutput output) {
        contextRunner.run(context -> {
            DemoFeignClient client = context.getBean(DemoFeignClient.class);

            ResponseEntity<DemoPayload> response = client.findById(1L);

            assertThat(response.getBody()).isNotNull();
            assertThat(AopUtils.isAopProxy(client)).isTrue();
        });

        assertThat(output.getOut()).contains("[demo-client GET /users/{id}] ==> args:");
        assertThat(output.getOut()).contains("\"id\":1");
        assertThat(output.getOut()).contains("[demo-client GET /users/{id}] <== cost:");
        assertThat(output.getOut()).contains("\"userName\":\"tom\"");
    }

    @Test
    void shouldTruncatePayloadsUsingConfiguredLimit(CapturedOutput output) {
        contextRunner
                .withPropertyValues("velo.feign.request-logging-max-payload-length=13")
                .run(context -> context.getBean(DemoFeignClient.class).findById(1L));

        assertThat(output.getOut()).contains("[demo-client GET /users/{id}] ==> args:");
        assertThat(output.getOut()).contains("resp: {\"userName...");
    }

    @Test
    void shouldAllowUnlimitedPayloadLoggingWhenConfiguredAsNegativeOne(CapturedOutput output) {
        contextRunner
                .withPropertyValues("velo.feign.request-logging-max-payload-length=-1")
                .run(context -> context.getBean(DemoFeignClient.class).findById(1L));

        assertThat(output.getOut()).contains("[demo-client GET /users/{id}] ==> args:");
        assertThat(output.getOut()).contains("resp: {\"userName\":\"tom\"}");
        assertThat(output.getOut()).doesNotContain("...");
    }

    @Test
    void shouldLogDashWhenResponseBodyIsNull(CapturedOutput output) {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        VeloCoreAutoConfiguration.class,
                        VeloFeignAutoConfiguration.class
                ))
                .withUserConfiguration(NullBodyFeignConfiguration.class)
                .run(context -> context.getBean(NullBodyFeignClient.class).ping());

        assertThat(output.getOut()).contains("[null-body-client GET /ping] <== cost:");
        assertThat(output.getOut()).contains("resp: -");
    }

    @Test
    void shouldLogFailuresAtErrorLevel(CapturedOutput output) {
        contextRunner.run(context -> {
            DemoFeignClient client = context.getBean(DemoFeignClient.class);
            try {
                client.fail();
            } catch (IllegalStateException ignored) {
            }
        });

        assertThat(output.getOut()).contains("[demo-client GET /users/fail] <!! failed: boom");
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
}
