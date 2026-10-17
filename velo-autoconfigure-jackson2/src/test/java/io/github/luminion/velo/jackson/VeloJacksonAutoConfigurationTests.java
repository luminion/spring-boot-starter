package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class VeloJacksonAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloJacksonAutoConfiguration.class, VeloRedisJsonAutoConfiguration.class));

    @Test
    void shouldKeepTheSharedCacheSerializerWhenRedisTemplatesAreDisabled() {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .withPropertyValues("velo.redis.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(RedisSerializer.class));
    }

    @Test
    void shouldBackOffRedisSerializerWhenUserProvidesDifferentBeanName() {
        RedisSerializer<Object> userSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object value) {
                return new byte[]{1};
            }

            @Override
            public Object deserialize(byte[] bytes) {
                return "custom";
            }
        };

        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .withBean("userRedisSerializer", RedisSerializer.class, () -> userSerializer)
                .run(context -> {
                    assertThat(context).hasSingleBean(RedisSerializer.class);
                    assertThat(context.getBean(RedisSerializer.class)).isSameAs(userSerializer);
                });
    }

    @Test
    void shouldSerializeLongAsStringAndSerializeBigDecimalAsStringByDefault() throws Exception {
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

                    assertThat(tree.get("id").isTextual()).isTrue();
                    assertThat(tree.get("id").textValue()).isEqualTo("1");
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
    void shouldSerializeBigIntegerAsStringUnconditionallyEvenForSmallValues() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    // 小值 BigInteger 也应转字符串，保证与 Long 行为一致、契约稳定
                    JsonNode small = objectMapper.readTree(objectMapper.writeValueAsString(
                            new BigIntegerPayload(java.math.BigInteger.valueOf(5))));
                    assertThat(small.get("value").isTextual()).isTrue();
                    assertThat(small.get("value").textValue()).isEqualTo("5");

                    JsonNode large = objectMapper.readTree(objectMapper.writeValueAsString(
                            new BigIntegerPayload(new java.math.BigInteger("100000000000000000000"))));
                    assertThat(large.get("value").isTextual()).isTrue();
                    assertThat(large.get("value").textValue()).isEqualTo("100000000000000000000");
                });
    }

    @Test
    void shouldAllowDisablingLongStringSerialization() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setSerializeLongAsString(false);

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

                    assertThat(tree.get("id").isNumber()).isTrue();
                    assertThat(tree.get("id").longValue()).isEqualTo(1L);
                    assertThat(tree.get("unsafeId").isNumber()).isTrue();
                    assertThat(tree.get("unsafeId").longValue()).isEqualTo(9007199254740992L);
                });
    }

    @Test
    void shouldAllowDisablingBigDecimalStringSerialization() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setSerializeBigDecimalAsString(false);

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
    void shouldKeepBigDecimalTrailingZerosByDefault() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.4500"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56))));

                    assertThat(tree.get("amount").textValue()).isEqualTo("123.4500");
                });
    }

    @Test
    void shouldStripBigDecimalTrailingZerosWhenSerializingAsString() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setBigDecimalStripTrailingZeros(true);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.4500"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56))));

                    assertThat(tree.get("amount").isTextual()).isTrue();
                    assertThat(tree.get("amount").textValue()).isEqualTo("123.45");
                });
    }

    @Test
    void shouldStripBigDecimalTrailingZerosWhenSerializingAsNumber() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setSerializeBigDecimalAsString(false);
        properties.getJackson().setBigDecimalStripTrailingZeros(true);

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    String json = objectMapper.writeValueAsString(new SamplePayload(
                            1L,
                            9007199254740992L,
                            new BigDecimal("123.4500"),
                            0.5D,
                            0.25F,
                            LocalDateTime.of(2026, 3, 30, 12, 34, 56)));
                    JsonNode tree = objectMapper.readTree(json);

                    assertThat(tree.get("amount").isNumber()).isTrue();
                    assertThat(tree.get("amount").decimalValue()).isEqualByComparingTo(new BigDecimal("123.45"));
                    assertThat(json).contains("\"amount\":123.45");
                });
    }

    @Test
    void shouldSerializeScientificBigDecimalAsPlainTextByDefault() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new ScientificAmountPayload(
                            new BigDecimal("1E+3"))));

                    assertThat(tree.get("amount").isTextual()).isTrue();
                    assertThat(tree.get("amount").textValue()).isEqualTo("1000");
                });
    }

    @Test
    void shouldStillSerializeScientificBigDecimalAsPlainTextWhenSpringPlainFeatureIsEnabled() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
                    context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer.class)
                            .values()
                            .forEach(customizer -> customizer.customize(builder));
                    builder.featuresToEnable(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
                    ObjectMapper objectMapper = builder.build();
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(new ScientificAmountPayload(
                            new BigDecimal("1E+3"))));

                    assertThat(tree.get("amount").isTextual()).isTrue();
                    assertThat(tree.get("amount").textValue()).isEqualTo("1000");
                });
    }

    @Test
    void shouldSerializeFloatingPointValuesAsStringWhenConfigured() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setSerializeFloatingAsString(true);

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

                    assertThat(tree.get("id").isTextual()).isTrue();
                    assertThat(tree.get("id").textValue()).isEqualTo("1");
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
    void shouldIgnoreInvalidDateTimePatternsWhenJacksonDateTimeCustomizationDisabled() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setDateTimeEnabled(false);
        properties.getDateTimeFormat().setDateTime("invalid[");
        properties.getDateTimeFormat().setDate("invalid[");
        properties.getDateTimeFormat().setTime("invalid[");

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
                });
    }

    @Test
    void shouldUseFlexibleDefaultDateFormatAndHonorJsonFormat() throws Exception {
        contextRunner
                .withBean(VeloProperties.class, VeloProperties::new)
                .run(context -> {
                    ObjectMapper objectMapper = objectMapper(context);
                    DatePayload payload = objectMapper.readValue(
                            "{\"dateOnly\":\"2024-01-02\",\"dateTime\":\"2024-01-02 03:04:05\","
                                    + "\"annotatedDate\":\"2024/03/31\","
                                    + "\"annotatedDateTime\":\"2024/03/31 08:09\"}",
                            DatePayload.class);

                    ZoneId defaultZone = ZoneId.of("GMT+8");
                    assertThat(payload.getDateOnly()).isEqualTo(Date.from(
                            LocalDate.of(2024, 1, 2).atStartOfDay(defaultZone).toInstant()));
                    assertThat(payload.getDateTime()).isEqualTo(Date.from(
                            LocalDateTime.of(2024, 1, 2, 3, 4, 5).atZone(defaultZone).toInstant()));
                    assertThat(payload.getAnnotatedDate()).isEqualTo(Date.from(
                            LocalDate.of(2024, 3, 31).atStartOfDay(ZoneOffset.UTC).toInstant()));
                    assertThat(payload.getAnnotatedDateTime()).isEqualTo(LocalDateTime.of(2024, 3, 31, 8, 9));

                    String json = objectMapper.writeValueAsString(payload);
                    assertThat(json).contains("\"dateOnly\":\"2024-01-02 00:00:00\"");
                    assertThat(json).contains("\"dateTime\":\"2024-01-02 03:04:05\"");
                    assertThat(json).contains("\"annotatedDate\":\"2024/03/31\"");
                    assertThat(json).contains("\"annotatedDateTime\":\"2024/03/31 08:09\"");
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
    void shouldUseExplicitJsonEnumFieldsWhenGlobalMappingsAreEmpty() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getJackson().setEnumMappings(Collections.emptyMap());

        contextRunner
                .withBean(VeloProperties.class, () -> properties)
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
    void shouldApplyStringAnnotationsAndConfiguredJacksonXss() {
        VeloProperties properties = new VeloProperties();
        properties.getXss().setJacksonEnabled(true);
        contextRunner
                .withBean(VeloProperties.class, () -> properties)
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

    @Test
    void shouldKeepUnannotatedStringsUnchangedWhenJacksonXssTargetIsDisabled() {
        VeloProperties properties = new VeloProperties();
        properties.getXss().setJacksonEnabled(false);
        contextRunner
                .withBean(VeloProperties.class, () -> properties)
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
                    StringTransformPayload payload = objectMapper.readValue(
                            "{\"encoded\":\"abc\",\"decoded\":\"ABC\",\"cleaned\":\"<b>safe</b>\",\"ignored\":\"<b>raw</b>\"}",
                            StringTransformPayload.class);
                    JsonNode tree = objectMapper.readTree(objectMapper.writeValueAsString(payload));

                    assertThat(payload.getDecoded()).isEqualTo("abc");
                    assertThat(payload.getCleaned()).isEqualTo("<b>safe</b>");
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
        builder.modulesToInstall(context.getBeansOfType(com.fasterxml.jackson.databind.Module.class)
                .values().toArray(new com.fasterxml.jackson.databind.Module[0]));
        context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer.class)
                .values()
                .forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    static class SamplePayload {
        /** 普通业务标识，用于验证长整数默认编码。 */
        private final Long id;

        /** 超出 JavaScript 安全范围的标识，用于验证精度保护。 */
        private final Long unsafeId;

        /** 金额字段，用于验证 BigDecimal 默认编码。 */
        private final BigDecimal amount;

        /** 比例字段，用于验证浮点数保持数值形态。 */
        private final Double ratio;

        /** 得分字段，用于验证 Float 保持数值形态。 */
        private final Float score;

        /** 创建时间，用于验证日期时间格式化。 */
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

    static class DatePayload {
        /** 按默认日期格式处理的日期。 */
        private Date dateOnly;

        /** 按默认日期时间格式处理的日期。 */
        private Date dateTime;

        /** 按属性注解格式处理的日期。 */
        @JsonFormat(pattern = "yyyy/MM/dd", timezone = "UTC")
        private Date annotatedDate;

        /** 按属性注解格式处理的本地日期时间。 */
        @JsonFormat(pattern = "yyyy/MM/dd HH:mm")
        private LocalDateTime annotatedDateTime;

        public DatePayload() {
        }

        public Date getDateOnly() {
            return dateOnly;
        }

        public void setDateOnly(Date dateOnly) {
            this.dateOnly = dateOnly;
        }

        public Date getDateTime() {
            return dateTime;
        }

        public void setDateTime(Date dateTime) {
            this.dateTime = dateTime;
        }

        public Date getAnnotatedDate() {
            return annotatedDate;
        }

        public void setAnnotatedDate(Date annotatedDate) {
            this.annotatedDate = annotatedDate;
        }

        public LocalDateTime getAnnotatedDateTime() {
            return annotatedDateTime;
        }

        public void setAnnotatedDateTime(LocalDateTime annotatedDateTime) {
            this.annotatedDateTime = annotatedDateTime;
        }
    }

    static class ScientificAmountPayload {
        /** 使用科学计数法构造、用于验证普通十进制输出的金额。 */
        private final BigDecimal amount;

        ScientificAmountPayload(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }

    static class BigIntegerPayload {
        /** 用于验证大整数的字符串序列化行为。 */
        private final java.math.BigInteger value;

        BigIntegerPayload(java.math.BigInteger value) {
            this.value = value;
        }

        public java.math.BigInteger getValue() {
            return value;
        }
    }

    static class EnumPayload {
        /** 用于根据枚举编码生成描述属性的状态值。 */
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
        /** 使用旧版键和值字段约定的状态值。 */
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
        /** 使用显式编码字段、描述字段和属性后缀的状态值。 */
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
        /** 仅配置编码字段映射的状态值。 */
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
        /** 标记为忽略 XSS 清洗的字符串属性。 */
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
        /** 通过编码处理器转换的字符串。 */
        @JsonEncode(UppercaseProcessor.class)
        private String encoded;

        /** 通过解码处理器转换的字符串。 */
        @JsonDecode(LowercaseProcessor.class)
        private String decoded;

        /** 根据全局配置清洗的字符串。 */
        private String cleaned;

        /** 标记为跳过 XSS 清洗的字符串。 */
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

        /** 枚举的稳定业务编码。 */
        private final int code;

        /** 面向展示的枚举描述。 */
        private final String name;

        StatusEnum(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    enum LegacyStatusEnum {
        ENABLED(1, "Enabled");

        /** 旧版枚举约定中的编码字段。 */
        private final int key;

        /** 旧版枚举约定中的描述字段。 */
        private final String value;

        LegacyStatusEnum(int key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    enum ExplicitStatusEnum {
        ENABLED(1, "Enabled");

        /** 显式指定的业务编码。 */
        private final int id;

        /** 显式指定的展示名称。 */
        private final String label;

        ExplicitStatusEnum(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    enum CodeOnlyStatusEnum {
        ENABLED(1);

        /** 仅提供业务编码的枚举值。 */
        private final int code;

        CodeOnlyStatusEnum(int code) {
            this.code = code;
        }
    }
}
