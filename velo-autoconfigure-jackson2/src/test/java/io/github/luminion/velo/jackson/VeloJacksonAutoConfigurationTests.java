package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.jackson.annotation.JsonDecode;
import io.github.luminion.velo.jackson.annotation.JsonEncode;
import io.github.luminion.velo.jackson.annotation.JsonEnum;
import io.github.luminion.velo.spi.JsonProcessorProvider;
import io.github.luminion.velo.xss.XssCleaner;
import io.github.luminion.velo.xss.XssIgnore;
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
        properties.getJackson().setBigDecimalAsString(false);

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
        properties.getJackson().setFloatingAsString(true);

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
    void shouldAllowDisablingJacksonDateTimeCustomization() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setDateTimeEnabled(false);

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

                    assertThat(json).contains("\"createdAt\":[2026,3,30,12,34,56]");
                    assertThat(json).doesNotContain("\"createdAt\":\"2026-03-30 12:34:56\"");
                });
    }

    @Test
    void shouldSerializeJsonEnumDerivedNameWithDefaultFields() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    JsonNode tree = objectMapper(context).readTree(objectMapper(context)
                            .writeValueAsString(new EnumPayload(1)));

                    assertThat(tree.get("status").intValue()).isEqualTo(1);
                    assertThat(tree.get("statusName").textValue()).isEqualTo("Enabled");
                });
    }

    @Test
    void shouldUseLegacyKeyValueEnumFieldsAsDefaultFallback() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new LegacyEnumPayload(1)));

                    assertThat(tree.get("statusName").textValue()).isEqualTo("Enabled");
                });
    }

    @Test
    void shouldUseExplicitJsonEnumFieldsAndSuffix() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new ExplicitEnumPayload(1)));

                    assertThat(tree.get("statusLabel").textValue()).isEqualTo("Enabled");
                });
    }

    @Test
    void shouldApplyJacksonNamingStrategyToDerivedEnumName() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context, PropertyNamingStrategy.SNAKE_CASE);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new EnumPayload(1)));

                    assertThat(tree.get("status_name").textValue()).isEqualTo("Enabled");
                    assertThat(tree.get("statusName")).isNull();
                });
    }

    @Test
    void shouldSkipJsonEnumDerivedNameWhenTargetFieldExists() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new ConflictingEnumPayload(1)));

                    assertThat(tree.get("statusName").textValue()).isEqualTo("Existing");
                });
    }

    @Test
    void shouldNotAccessJavaLangEnumNameWhenEnumNameFieldIsMissing() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new CodeOnlyEnumPayload(1)));

                    assertThat(tree.get("status").intValue()).isEqualTo(1);
                    assertThat(tree.get("statusName")).isNull();
                });
    }

    @Test
    void shouldDeserializeXssIgnoredStringWithoutJsonDecode() {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean(JsonProcessorProvider.class, () -> clazz -> {
                    try {
                        return clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    StringPayload payload = objectMapper.readValue("{\"name\":\"ok\"}", StringPayload.class);

                    assertThat(payload.getName()).isEqualTo("ok");
                });
    }

    @Test
    void shouldApplyStringEncodeDecodeAndXssCleaner() {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean(JsonProcessorProvider.class, () -> clazz -> {
                    try {
                        return clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .withBean(XssCleaner.class, () -> html -> html.replace("<b>", "").replace("</b>", ""))
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    StringTransformPayload payload = objectMapper
                            .readValue("{\"encoded\":\"abc\",\"decoded\":\"ABC\",\"cleaned\":\"<b>safe</b>\",\"ignored\":\"<b>raw</b>\"}",
                                    StringTransformPayload.class);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(payload));

                    assertThat(payload.getDecoded()).isEqualTo("abc");
                    assertThat(payload.getCleaned()).isEqualTo("safe");
                    assertThat(payload.getIgnored()).isEqualTo("<b>raw</b>");
                    assertThat(tree.get("encoded").textValue()).isEqualTo("ABC");
                });
    }

    private static ObjectMapper objectMapper(org.springframework.context.ApplicationContext context) {
        return objectMapper(context, null);
    }

    private static ObjectMapper objectMapper(org.springframework.context.ApplicationContext context,
                                             PropertyNamingStrategy propertyNamingStrategy) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        if (propertyNamingStrategy != null) {
            builder.propertyNamingStrategy(propertyNamingStrategy);
        }
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

    static class EnumPayload {
        @JsonEnum(StatusEnum.class)
        private final Integer status;

        EnumPayload(Integer status) {
            this.status = status;
        }

        public Integer getStatus() {
            return status;
        }
    }

    static class LegacyEnumPayload {
        @JsonEnum(LegacyStatusEnum.class)
        private final Integer status;

        LegacyEnumPayload(Integer status) {
            this.status = status;
        }

        public Integer getStatus() {
            return status;
        }
    }

    static class ExplicitEnumPayload {
        @JsonEnum(value = ExplicitStatusEnum.class, codeField = "id", nameField = "label", nameSuffix = "label")
        private final Integer status;

        ExplicitEnumPayload(Integer status) {
            this.status = status;
        }

        public Integer getStatus() {
            return status;
        }
    }

    static class ConflictingEnumPayload extends EnumPayload {
        ConflictingEnumPayload(Integer status) {
            super(status);
        }

        public String getStatusName() {
            return "Existing";
        }
    }

    static class CodeOnlyEnumPayload {
        @JsonEnum(CodeOnlyStatusEnum.class)
        private final Integer status;

        CodeOnlyEnumPayload(Integer status) {
            this.status = status;
        }

        public Integer getStatus() {
            return status;
        }
    }

    static class StringPayload {
        @XssIgnore
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class StringTransformPayload {
        @JsonEncode(UppercaseProcessor.class)
        private String encoded;
        @JsonDecode(LowercaseProcessor.class)
        private String decoded;
        private String cleaned;
        @XssIgnore
        private String ignored;

        public String getEncoded() {
            return encoded;
        }

        public void setEncoded(String encoded) {
            this.encoded = encoded;
        }

        public String getDecoded() {
            return decoded;
        }

        public void setDecoded(String decoded) {
            this.decoded = decoded;
        }

        public String getCleaned() {
            return cleaned;
        }

        public void setCleaned(String cleaned) {
            this.cleaned = cleaned;
        }

        public String getIgnored() {
            return ignored;
        }

        public void setIgnored(String ignored) {
            this.ignored = ignored;
        }
    }

    public static class UppercaseProcessor implements java.util.function.Function<String, String> {
        @Override
        public String apply(String value) {
            return value == null ? null : value.toUpperCase();
        }
    }

    public static class LowercaseProcessor implements java.util.function.Function<String, String> {
        @Override
        public String apply(String value) {
            return value == null ? null : value.toLowerCase();
        }
    }

    enum StatusEnum {
        ENABLED(1, "Enabled");

        private final int code;
        private final String name;

        StatusEnum(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    enum LegacyStatusEnum {
        ENABLED(1, "Enabled");

        private final int key;
        private final String value;

        LegacyStatusEnum(int key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    enum ExplicitStatusEnum {
        ENABLED(1, "Enabled");

        private final int id;
        private final String label;

        ExplicitStatusEnum(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    enum CodeOnlyStatusEnum {
        ENABLED(1);

        private final int code;

        CodeOnlyStatusEnum(int code) {
            this.code = code;
        }
    }
}
