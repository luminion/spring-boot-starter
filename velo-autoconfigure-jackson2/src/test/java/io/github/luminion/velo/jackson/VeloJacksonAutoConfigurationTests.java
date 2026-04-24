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
    void shouldSerializeUnsafeIntegerAsStringAndSerializeBigDecimalAsStringByDefault() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    String json = objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("1234567890.123456789"),
                            0.125D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56)));
                    JsonNode tree = objectMapper.readTree(json);

                    assertThat(tree.get("id").isNumber()).isTrue();
                    assertThat(tree.get("id").longValue()).isEqualTo(1L);
                    assertThat(tree.get("unsafeId").isTextual()).isTrue();
                    assertThat(tree.get("unsafeId").textValue()).isEqualTo("9007199254740992");
                    assertThat(tree.get("amount").isTextual()).isTrue();
                    assertThat(tree.get("amount").textValue()).isEqualTo("1234567890.123456789");
                    assertThat(json).contains("\"ratio\":0.125");
                    assertThat(json).doesNotContain("\"ratio\":\"");
                    assertThat(json).contains("\"score\":0.25");
                    assertThat(json).doesNotContain("\"score\":\"");
                    assertThat(json).contains("\"createdAt\":\"2026-03-30 12:34:56\"");
                    assertThat(context).hasSingleBean(RedisSerializer.class);
                });
    }

    @Test
    void shouldAllowDisablingBigDecimalStringSerialization() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setWriteBigDecimalAsString(false);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.45"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56))));

                    assertThat(tree.get("amount").isNumber()).isTrue();
                    assertThat(tree.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("123.45"));
                });
    }

    @Test
    void shouldSerializeFloatingPointValuesAsStringWhenConfigured() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setWriteFloatingPointAsString(true);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    String json = objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.45"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56)));
                    JsonNode tree = objectMapper.readTree(json);

                    assertThat(tree.get("id").isNumber()).isTrue();
                    assertThat(tree.get("id").longValue()).isEqualTo(1L);
                    assertThat(json).contains("\"unsafeId\":\"9007199254740992\"");
                    assertThat(json).contains("\"amount\":\"123.45\"");
                    assertThat(json).contains("\"ratio\":\"0.5\"");
                    assertThat(json).contains("\"score\":\"0.25\"");
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
                            0.25F,
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
        private final Float score;
        private final LocalDateTime createdAt;

        SamplePayload(Long id, Long unsafeId, BigDecimal amount, Double ratio, Float score, LocalDateTime createdAt) {
            this.id = id;
            this.unsafeId = unsafeId;
            this.amount = amount;
            this.ratio = ratio;
            this.score = score;
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

        public Float getScore() {
            return score;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
