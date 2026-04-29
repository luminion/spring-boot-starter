package io.github.luminion.velo.jackson;

import io.github.luminion.velo.VeloProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
                    JsonMapper mapper = jsonMapper(context);
                    String json = mapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("1234567890.123456789"),
                            0.125D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 31, 8, 9, 10)));
                    JsonNode tree = mapper.readTree(json);

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
                    assertThat(json).contains("\"createdAt\":\"2026-03-31 08:09:10\"");
                    assertThat(context).hasSingleBean(RedisSerializer.class);
                });
    }

    @Test
    void shouldAllowDisablingBigDecimalStringSerialization() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setBigDecimalAsString(false);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    JsonMapper mapper = jsonMapper(context);
                    JsonNode tree = mapper.readTree(mapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.45"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 31, 8, 9, 10))));

                    assertThat(tree.get("amount").isNumber()).isTrue();
                    assertThat(tree.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("123.45"));
                });
    }

    @Test
    void shouldSerializeFloatingPointValuesAsStringWhenConfigured() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setFloatingAsString(true);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    JsonMapper mapper = jsonMapper(context);
                    String json = mapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.45"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 31, 8, 9, 10)));
                    JsonNode tree = mapper.readTree(json);

                    assertThat(tree.get("id").isNumber()).isTrue();
                    assertThat(tree.get("id").longValue()).isEqualTo(1L);
                    assertThat(json).contains("\"unsafeId\":\"9007199254740992\"");
                    assertThat(json).contains("\"amount\":\"123.45\"");
                    assertThat(json).contains("\"ratio\":\"0.5\"");
                    assertThat(json).contains("\"score\":\"0.25\"");
                });
    }

    private static JsonMapper jsonMapper(org.springframework.context.ApplicationContext context) {
        JsonMapper.Builder builder = JsonMapper.builder();
        context.getBeansOfType(JsonMapperBuilderCustomizer.class)
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
