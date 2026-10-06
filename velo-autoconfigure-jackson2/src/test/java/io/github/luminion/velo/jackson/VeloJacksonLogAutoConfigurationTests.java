package io.github.luminion.velo.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.LogValueFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class VeloJacksonLogAutoConfigurationTests {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(VeloJacksonLogAutoConfiguration.class));

    @Test
    void usesApplicationMapperWithoutServletAndRespectsIgnoredFields() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Payload.class, PayloadMixin.class);
        runner
                .withBean(ObjectMapper.class, () -> mapper)
                .run(
                        c -> {
                            assertThat(c).hasSingleBean(LogValueFormatter.class);
                            assertThat(
                                    InvocationLogSupport.format(new Payload("visible", "secret"),
                                            c.getBean(LogValueFormatter.class), -1))
                                    .isEqualTo("{\"value\":\"visible\"}");
                        });
    }

    @Test
    void noMapperLeavesFallbackToCommonLogConfiguration() {
        runner.run(c -> assertThat(c).doesNotHaveBean(LogValueFormatter.class));
    }

    @Test
    void userFormatterWins() {
        LogValueFormatter custom = (value, output) -> output.write("custom");
        runner
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(LogValueFormatter.class, () -> custom)
                .run(c -> assertThat(c.getBean(LogValueFormatter.class)).isSameAs(custom));
    }

    @Test
    void largeDtoCollectionStopsBeforeFullTraversalWithoutReadingIgnoredGetter() {
        AtomicInteger visited = new AtomicInteger();
        runner.withBean(ObjectMapper.class, ObjectMapper::new).run(c -> {
            String text = InvocationLogSupport.format(new LargePayload(visited),
                    c.getBean(LogValueFormatter.class), 64);
            assertThat(text.length()).isLessThanOrEqualTo(64);
            assertThat(text).startsWith("{\"values\":[").endsWith("...");
            assertThat(visited.get()).isPositive().isLessThan(2000);
        });
    }

    @Test
    void longStringLimitDoesNotChangeTheApplicationMapper() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String value = String.join("", Collections.nCopies(10000, "x"));
        runner.withBean(ObjectMapper.class, () -> mapper).run(c -> {
            String text = InvocationLogSupport.format(value, c.getBean(LogValueFormatter.class), 64);
            assertThat(text).hasSize(64).startsWith("\"").endsWith("...");
        });
        assertThat(mapper.writeValueAsString(value)).hasSize(value.length() + 2);
    }

    static class LargePayload {
        private final AtomicInteger visited;

        LargePayload(AtomicInteger visited) {
            this.visited = visited;
        }

        public List<CountingPayload> getValues() {
            return Collections.nCopies(100000, new CountingPayload(visited));
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
                                            new BrokenPayload(), c.getBean(LogValueFormatter.class), -1))
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
