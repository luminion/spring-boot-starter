package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.luminion.velo.core.VeloProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class VeloJacksonAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloJacksonAutoConfiguration.class));

    @Test
    void shouldSerializeUnsafeIntegerAsStringAndKeepDecimalAsNumberByDefault() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    String json = objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("1234567890.123456789"),
                            0.125D,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56)));
                    JsonNode tree = objectMapper.readTree(json);

                    assertThat(tree.get("id").isNumber()).isTrue();
                    assertThat(tree.get("id").longValue()).isEqualTo(1L);
                    assertThat(tree.get("unsafeId").isTextual()).isTrue();
                    assertThat(tree.get("unsafeId").textValue()).isEqualTo("9007199254740992");
                    assertThat(json).contains("\"amount\":1234567890.123456789");
                    assertThat(json).doesNotContain("\"amount\":\"");
                    assertThat(json).contains("\"ratio\":0.125");
                    assertThat(json).doesNotContain("\"ratio\":\"");
                    assertThat(json).contains("\"createdAt\":\"2026-03-30 12:34:56\"");
                    assertThat(context).hasSingleBean(RedisSerializer.class);
                });
    }

    @Test
    void shouldSerializeConfiguredNumberTypesAsString() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setWriteIntegerAsString(true);
        properties.getJackson().setWriteBigDecimalAsString(true);
        properties.getJackson().setWriteFloatingPointAsString(true);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    String json = objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            2L,
                            new BigDecimal("123.45"),
                            0.5D,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56)));

                    assertThat(json).contains("\"id\":\"1\"");
                    assertThat(json).contains("\"unsafeId\":\"2\"");
                    assertThat(json).contains("\"amount\":\"123.45\"");
                    assertThat(json).contains("\"ratio\":\"0.5\"");
                });
    }

    @Test
    void shouldAllowDisablingJacksonDateTimeSerializers() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().getDateTime().setSerializersEnabled(false);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    String json = objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            2L,
                            new BigDecimal("123.45"),
                            0.5D,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56)));

                    assertThat(json).contains("\"createdAt\":\"2026-03-30T12:34:56\"");
                    assertThat(json).doesNotContain("\"createdAt\":\"2026-03-30 12:34:56\"");
                });
    }

    private static ObjectMapper objectMapper(org.springframework.context.ApplicationContext context) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer.class)
                .values()
                .forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    static class SamplePayload {
        private final Long id;
        private final Long unsafeId;
        private final BigDecimal amount;
        private final Double ratio;
        private final LocalDateTime createdAt;

        SamplePayload(Long id, Long unsafeId, BigDecimal amount, Double ratio, LocalDateTime createdAt) {
            this.id = id;
            this.unsafeId = unsafeId;
            this.amount = amount;
            this.ratio = ratio;
            this.createdAt = createdAt;
        }

        public Long getId() {
            return id;
        }

        public Long getUnsafeId() {
            return unsafeId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public Double getRatio() {
            return ratio;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
