package io.github.luminion.velo.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.log.InvocationLogSupport;
import io.github.luminion.velo.log.LogValueFormatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
                                    c.getBean(LogValueFormatter.class).format(new Payload("visible", "secret")))
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
