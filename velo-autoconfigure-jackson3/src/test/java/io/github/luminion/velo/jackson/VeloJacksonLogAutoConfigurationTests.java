package io.github.luminion.velo.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.luminion.velo.log.core.InvocationLogSupport;
import io.github.luminion.velo.log.LogValueFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class VeloJacksonLogAutoConfigurationTests {
    @Test
    void springMapperAndApplicationModuleAreReadyBeforeLoggingFormatter() {
        tools.jackson.databind.module.SimpleModule module = new tools.jackson.databind.module.SimpleModule();
        module.setMixInAnnotation(Payload.class, PayloadMixin.class);
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class,
                        VeloJacksonLogAutoConfiguration.class,
                        io.github.luminion.velo.core.VeloCoreAutoConfiguration.class,
                        io.github.luminion.velo.log.config.VeloLogAutoConfiguration.class))
                .withBean(tools.jackson.databind.module.SimpleModule.class, () -> module)
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(LogValueFormatter.class);
                    String text = InvocationLogSupport.format(new Payload("visible", "secret"),
                            context.getBean(LogValueFormatter.class));
                    assertThat(text).isEqualTo("{\"value\":\"visible\"}");
                });
    }

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(VeloJacksonLogAutoConfiguration.class));

    @Test
    void usesApplicationMapperWithoutServletAndRespectsIgnoredFields() {
        ObjectMapper mapper = JsonMapper.builder().addMixIn(Payload.class, PayloadMixin.class).build();
        runner
                .withBean(ObjectMapper.class, () -> mapper)
                .run(
                        c -> {
                            assertThat(c).hasSingleBean(LogValueFormatter.class);
                            assertThat(
                                    InvocationLogSupport.format(new Payload("visible", "secret"),
                                            c.getBean(LogValueFormatter.class)))
                                    .isEqualTo("{\"value\":\"visible\"}");
                        });
    }

    @Test
    void noMapperLeavesFallbackToCommonLogConfiguration() {
        runner.run(c -> assertThat(c).doesNotHaveBean(LogValueFormatter.class));
    }

    @Test
    void userFormatterWins() {
        LogValueFormatter custom = value -> "custom";
        runner
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(LogValueFormatter.class, () -> custom)
                .run(c -> assertThat(c.getBean(LogValueFormatter.class)).isSameAs(custom));
    }

    @Test
    void largeDtoCollectionIsFullySerializedWithoutReadingIgnoredGetter() {
        AtomicInteger visited = new AtomicInteger();
        runner.withBean(ObjectMapper.class, ObjectMapper::new).run(c -> {
            String text = InvocationLogSupport.format(new LargePayload(visited),
                    c.getBean(LogValueFormatter.class));
            assertThat(text).startsWith("{\"values\":[").endsWith("{\"value\":1000}]}");
            assertThat(visited).hasValue(1000);
        });
    }

    @Test
    void longStringIsCompleteAndDoesNotChangeTheApplicationMapper() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = String.join("", Collections.nCopies(10000, "x"));
        runner.withBean(ObjectMapper.class, () -> mapper).run(c -> {
            String text = InvocationLogSupport.format(value, c.getBean(LogValueFormatter.class));
            assertThat(text).isEqualTo("\"" + value + "\"");
        });
        assertThat(mapper.writeValueAsString(value)).hasSize(value.length() + 2);
    }

    static class LargePayload {
        private final AtomicInteger visited;

        LargePayload(AtomicInteger visited) {
            this.visited = visited;
        }

        public List<CountingPayload> getValues() {
            return Collections.nCopies(1000, new CountingPayload(visited));
        }
    }

    static class CountingPayload {
        private final AtomicInteger visited;

        CountingPayload(AtomicInteger visited) {
            this.visited = visited;
        }

        public int getValue() {
            return visited.incrementAndGet();
        }

        @JsonIgnore
        public String getSecret() {
            throw new AssertionError("Ignored getter must not be read");
        }
    }

    @Test
    void serializationFailureNeverCallsToString() {
        runner
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(
                        c -> {
                            assertThat(
                                    InvocationLogSupport.format(
                                            new BrokenPayload(), c.getBean(LogValueFormatter.class)))
                                    .isEqualTo("serialization-failed");
                        });
    }

    @Getter
    @RequiredArgsConstructor
    static class Payload {
        private final String value;
        private final String secret;
    }

    abstract static class PayloadMixin {
        @JsonIgnore
        abstract String getSecret();
    }

    static class BrokenPayload {
        public String getValue() {
            throw new IllegalArgumentException("broken");
        }

        @Override
        public String toString() {
            throw new AssertionError("must not expose fallback");
        }
    }
}
