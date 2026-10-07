package io.github.luminion.velo.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.PropertyNamingStrategy;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.annotation.JsonNaming;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.ser.std.ToStringSerializerBase;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.jackson.annotation.JsonEncode;
import io.github.luminion.velo.jackson.annotation.JsonEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 Velo 与 Boot 原生自动配置叠加后的实际行为。
 */
class JacksonIntegrationRegressionTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                    VeloJacksonAutoConfiguration.class, VeloRedisJsonAutoConfiguration.class, JacksonAutoConfiguration.class));

    @Test
    void shouldRegisterEnumModuleThroughBoot() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new EnumPayload())).isEqualTo("{\"status\":1,\"statusName\":\"One\"}");
        });
        runner.withPropertyValues("velo.jackson.enum-desc-enabled=false").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new EnumPayload())).isEqualTo("{\"status\":1}");
        });
    }

    @Test
    void shouldKeepBusinessModuleAndEnumModuleTogether() {
        runner.withBean(tools.jackson.databind.JacksonModule.class, () ->
                new tools.jackson.databind.module.SimpleModule("business").addSerializer(new PrefixNumberSerializer())).run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(1L)).isEqualTo("\"n:1\"");
            assertThat(mapper.writeValueAsString(new EnumPayload()))
                    .isEqualTo("{\"status\":1,\"statusName\":\"One\"}");
        });
    }

    @Test
    void shouldPreserveNativePolymorphicStringHandling() {
        runner.run(context -> {
            // 读取 Object 多态字段时仅为被测 String 类型设置允许列表，不改动生产校验策略。
            ObjectMapper mapper = context.getBean(ObjectMapper.class).rebuild()
                    .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                            .allowIfSubType(String.class).build())
                    .build();
            String json = mapper.writeValueAsString(new PolymorphicPayload("abc"));
            assertThat(json).isEqualTo("{\"value\":\"abc\"}");
            assertThat(mapper.readValue(json, PolymorphicPayload.class).value).isEqualTo("abc");
        });
    }

    @Test
    void shouldEncodePolymorphicStringsWithoutLeakingPropertyContext() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            JsonNode tree = mapper.readTree(mapper.writeValueAsString(new EncodedStringPayload()));
            assertThat(tree.get("encoded").asText()).isEqualTo("ABC");
            assertThat(tree.get("plain").asText()).isEqualTo("abc");
            assertThat(mapper.writeValueAsString("abc")).isEqualTo("\"abc\"");
            assertThat(mapper.writeValueAsString(new String[]{"abc", null}))
                    .isEqualTo("[\"abc\",null]");
        });
    }

    @Test
    void shouldWriteNullEncodingResultsWithoutTypeWrappers() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new NullEncodedStringPayload()))
                    .isEqualTo("{\"encoded\":null}");
            NullEncodedStringPayload payload = new NullEncodedStringPayload();
            payload.encoded = null;
            assertThat(mapper.writeValueAsString(payload)).isEqualTo("{\"encoded\":null}");
        });
    }

    @Test
    void shouldPreserveNativeEmptyStringInclusion() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new EmptyStringPayload())).isEqualTo("{}");
        });
    }

    @Test
    void shouldRecognizeOffsetAndRegionTimeZones() {
        for (String zone : new String[]{"+08:00", "GMT+8", "GMT+08:00", "Asia/Shanghai"}) {
            runner.withPropertyValues("velo.date-time-format.time-zone=" + zone).run(context -> {
                ObjectMapper mapper = context.getBean(ObjectMapper.class);
                assertThat(mapper.writeValueAsString(new Date(0))).isEqualTo("\"1970-01-01 08:00:00\"");
                assertThat(mapper.readValue("\"1970-01-01 08:00:00\"", Date.class)).isEqualTo(new Date(0));
            });
        }
    }

    @Test
    void shouldFailInitializationForInvalidTimeZones() {
        for (String zone : new String[]{"Wrong/Zone", "+25:00"}) {
            runner.withPropertyValues("velo.date-time-format.time-zone=" + zone).run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(DateTimeException.class);
            });
        }
    }

    @Test
    void shouldApplyNumericDefaultsToPrimitiveAndObjectArrays() {
        runner.withPropertyValues("velo.jackson.serialize-floating-as-string=true").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new long[]{9007199254740993L})).isEqualTo("[\"9007199254740993\"]");
            assertThat(mapper.writeValueAsString(new Long[]{1L, null})).isEqualTo("[\"1\",null]");
            assertThat(mapper.writeValueAsString(new BigInteger[]{BigInteger.ONE})).isEqualTo("[\"1\"]");
            assertThat(mapper.writeValueAsString(new BigDecimal[]{new BigDecimal("12.300"), null}))
                    .isEqualTo("[\"12.300\",null]");
            assertThat(mapper.writeValueAsString(new double[]{0.5})).isEqualTo("[\"0.5\"]");
            assertThat(mapper.writeValueAsString(new float[]{0.25F})).isEqualTo("[\"0.25\"]");
            assertThat(mapper.writeValueAsString(new Double[]{0.5})).isEqualTo("[\"0.5\"]");
            assertThat(mapper.writeValueAsString(new Float[]{0.25F})).isEqualTo("[\"0.25\"]");
            assertThat(mapper.writeValueAsString(new long[0])).isEqualTo("[]");
            assertThat(mapper.writeValueAsString(new long[][]{{1L}, {2L}})).isEqualTo("[[\"1\"],[\"2\"]]");
            assertThat(mapper.writeValueAsString(new BigDecimal[][]{{new BigDecimal("1E+3")}}))
                    .isEqualTo("[[\"1000\"]]");
            assertThat(mapper.writeValueAsString(new int[]{1, 2})).isEqualTo("[1,2]");
        });
    }

    @Test
    void shouldRespectBootModuleDiscoverySetting() {
        for (boolean dates : new boolean[]{true, false}) {
            runner.withPropertyValues("spring.jackson.find-and-add-modules=false",
                    "velo.jackson.date-time-enabled=" + dates).run(context -> {
                ObjectMapper mapper = context.getBean(ObjectMapper.class);
                assertThat(mapper.writeValueAsString(new ServiceLoaderModuleFixture.DiscoveryPayload()))
                        .isEqualTo("{\"value\":\"native\"}");
                assertThat(mapper.writeValueAsString(LocalDate.of(2026, 10, 6)))
                        .isEqualTo("\"2026-10-06\"");
            });
        }
        runner.withPropertyValues("spring.jackson.find-and-add-modules=true").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new ServiceLoaderModuleFixture.DiscoveryPayload()))
                    .isEqualTo("\"discovered\"");
        });
    }

    @Test
    void shouldAllowFieldFormatToOverrideNumericDefaults() {
        runner.withPropertyValues("velo.jackson.serialize-floating-as-string=true").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            JsonNode tree = mapper.readTree(mapper.writeValueAsString(new NumericFormatPayload()));
            assertThat(tree.get("id").isNumber()).isTrue();
            assertThat(tree.get("bigInteger").isNumber()).isTrue();
            assertThat(tree.get("amount").isNumber()).isTrue();
            assertThat(tree.get("naturalAmount").isNumber()).isTrue();
            assertThat(tree.get("ratio").isNumber()).isTrue();
            assertThat(tree.get("longs").get(0).isNumber()).isTrue();
            assertThat(tree.get("amounts").get(0).isNumber()).isTrue();
            assertThat(tree.get("ratios").get(0).isNumber()).isTrue();
        });
    }

    @Test
    void shouldRestoreNativeNumbersWhenDefaultsAreDisabled() {
        runner.withPropertyValues("velo.jackson.serialize-long-as-string=false",
                "velo.jackson.serialize-big-decimal-as-string=false").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new long[]{1L})).isEqualTo("[1]");
            assertThat(mapper.writeValueAsString(new BigDecimal[]{new BigDecimal("12.30")})).isEqualTo("[12.30]");
            assertThat(mapper.writeValueAsString(new double[]{0.5})).isEqualTo("[0.5]");
            JsonNode tree = mapper.readTree(mapper.writeValueAsString(new ExplicitStringPayload()));
            assertThat(tree.get("id").isTextual()).isTrue();
            assertThat(tree.get("amount").isTextual()).isTrue();
        });
    }

    @Test
    void shouldHonorSingleElementArrayFormat() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new UnwrappedArrayPayload())).isEqualTo("{\"values\":\"1\"}");
        });
    }

    @Test
    void shouldKeepExplicitContentSerializerForPrimitiveArray() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new CustomContentPayload())).isEqualTo("{\"values\":[\"n:1\"]}");
        });
    }

    @Test
    void shouldApplyTrailingZeroOptionToBigDecimalArray() {
        runner.withPropertyValues("velo.jackson.big-decimal-strip-trailing-zeros=true").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new BigDecimal[]{new BigDecimal("12.300")})).isEqualTo("[\"12.3\"]");
        });
    }

    @Test
    void shouldKeepNativeTypeInformationForNumbers() {
        runner.run(context -> {
            // Jackson 3 默认限制 Object 多态读取，测试只允许本例中的三个数字类型。
            ObjectMapper mapper = context.getBean(ObjectMapper.class).rebuild()
                    .polymorphicTypeValidator(BasicPolymorphicTypeValidator.builder()
                            .allowIfSubType(Long.class).allowIfSubType(BigInteger.class)
                            .allowIfSubType(BigDecimal.class).build())
                    .build();
            for (Number value : new Number[]{1L, BigInteger.ONE, new BigDecimal("12.30")}) {
                String json = mapper.writeValueAsString(new PolymorphicPayload(value));
                PolymorphicPayload copy = mapper.readValue(json, PolymorphicPayload.class);
                assertThat(copy.value).isEqualTo(value);
                assertThat(copy.value).isExactlyInstanceOf(value.getClass());
            }
        });
    }

    @Test
    void shouldSuppressDerivedEnumNameWhenSourceIsDefault() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new DefaultEnumPayload())).isEqualTo("{}");
            EnumPayload unknown = new EnumPayload();
            unknown.status = 99;
            assertThat(mapper.writeValueAsString(unknown)).isEqualTo("{\"status\":99}");
        });
    }

    @Test
    void shouldWriteEnumNamesAndNullPlaceholdersInArrayShape() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writeValueAsString(new ArrayEnumPayload())).isEqualTo("[1,\"One\"]");
            ArrayEnumPayload unknown = new ArrayEnumPayload();
            unknown.status = 99;
            assertThat(mapper.writeValueAsString(unknown)).isEqualTo("[99,null]");
        });
    }

    @Test
    void shouldHonorClassNamingAndExplicitPropertyNames() {
        runner.withPropertyValues("spring.jackson.property-naming-strategy=KEBAB_CASE").run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            JsonNode tree = mapper.readTree(mapper.writeValueAsString(new NamedEnumPayload()));
            assertThat(tree.get("order_status_name").asText()).isEqualTo("One");
            assertThat(tree.get("state_code_label").asText()).isEqualTo("One");
            assertThat(tree.has("order_statusName")).isFalse();
        });
    }

    @Test
    void shouldPassRealMembersToCustomNamingStrategy() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            JsonNode tree = mapper.readTree(mapper.writeValueAsString(new CustomNamedEnumPayload()));
            assertThat(tree.get("prefix_statusName").asText()).isEqualTo("One");
            assertThat(tree.has("prefix_prefix_statusName")).isFalse();
        });
    }

    @Test
    void shouldKeepDerivedEnumFieldInSameViewAsSource() {
        runner.run(context -> {
            ObjectMapper mapper = context.getBean(ObjectMapper.class);
            assertThat(mapper.writerWithView(Visible.class).writeValueAsString(new ViewedEnumPayload()))
                    .isEqualTo("{\"status\":1,\"statusName\":\"One\"}");
            assertThat(mapper.writerWithView(Hidden.class).writeValueAsString(new ViewedEnumPayload()))
                    .isEqualTo("{}");
        });
    }

    @Test
    void shouldRoundTripFinalPojoAndJavaTimeThroughRedisSerializer() {
        runner.run(context -> {
            RedisSerializer serializer = context.getBean(io.github.luminion.velo.redis.RedisJsonSerializerFactory.class)
                    .genericCacheSerializer();
            CachedPayload payload = new CachedPayload(1L, LocalDate.of(2026, 10, 6),
                    LocalDateTime.of(2026, 10, 6, 12, 30));
            Object value = serializer.deserialize(serializer.serialize(payload));
            assertThat(value).isInstanceOf(CachedPayload.class);
            CachedPayload copy = (CachedPayload) value;
            assertThat(copy.id).isEqualTo(payload.id);
            assertThat(copy.date).isEqualTo(payload.date);
            assertThat(copy.timestamp).isEqualTo(payload.timestamp);
            assertThat(serializer.deserialize(serializer.serialize(null))).isNull();
        });
    }

    @Test
    void shouldReportMissingRedisTypeInformationInsteadOfReturningNull() {
        runner.run(context -> {
            RedisSerializer serializer = context.getBean(io.github.luminion.velo.redis.RedisJsonSerializerFactory.class)
                    .genericCacheSerializer();
            byte[] invalid = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> serializer.deserialize(invalid)).isInstanceOf(SerializationException.class);
        });
    }

    public static class EncodedStringPayload {
        @JsonEncode(UppercaseProcessor.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object encoded = "abc";
        public String plain = "abc";
    }

    public static class NullEncodedStringPayload {
        @JsonEncode(NullProcessor.class)
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object encoded = "abc";
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class EmptyStringPayload {
        @JsonEncode(UppercaseProcessor.class)
        public String encoded = "";
        public String plain = "";
    }

    public static class UppercaseProcessor implements Function<String, String> {
        @Override
        public String apply(String value) {
            return value.toUpperCase(Locale.ROOT);
        }
    }

    public static class NullProcessor implements Function<String, String> {
        @Override
        public String apply(String value) {
            assertThat(value).isNotNull();
            return null;
        }
    }

    public static class EnumPayload {
        @JsonEnum(Status.class)
        public int status = 1;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class DefaultEnumPayload {
        @JsonEnum(Status.class)
        public int status;
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    public static class ArrayEnumPayload extends EnumPayload {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class NamedEnumPayload {
        @JsonEnum(Status.class)
        public int orderStatus = 1;
        @JsonProperty("state_code")
        @JsonEnum(value = Status.class, nameSuffix = "label")
        public int status = 1;
    }

    @JsonNaming(MemberNamingStrategy.class)
    public static class CustomNamedEnumPayload {
        @JsonEnum(Status.class)
        private final int status = 1;

        public int getStatus() {
            return status;
        }
    }

    public static class MemberNamingStrategy extends PropertyNamingStrategy {
        @Override
        public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
            assertThat(field).isNotNull();
            return "prefix_" + defaultName;
        }

        @Override
        public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
            assertThat(method).isNotNull();
            return "prefix_" + defaultName;
        }
    }

    public static class NumericFormatPayload {
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public Long id = 1L;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public BigInteger bigInteger = BigInteger.ONE;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public BigDecimal amount = new BigDecimal("12.30");
        @JsonFormat(shape = JsonFormat.Shape.NATURAL)
        public BigDecimal naturalAmount = new BigDecimal("12.30");
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public double ratio = 0.5;
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public long[] longs = {1L};
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public BigDecimal[] amounts = {new BigDecimal("12.30")};
        @JsonFormat(shape = JsonFormat.Shape.NUMBER)
        public double[] ratios = {0.5};
    }

    public static class ExplicitStringPayload {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public Long id = 1L;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        public BigDecimal amount = new BigDecimal("12.30");
    }

    public static class UnwrappedArrayPayload {
        @JsonFormat(with = JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        public long[] values = {1L};
    }

    public static class CustomContentPayload {
        @JsonSerialize(contentUsing = PrefixNumberSerializer.class)
        public long[] values = {1L};
    }

    public static class PrefixNumberSerializer extends ToStringSerializerBase {
        public PrefixNumberSerializer() {
            super(Long.class);
        }

        @Override
        public String valueToString(Object value) {
            return "n:" + value;
        }
    }

    public static class ViewedEnumPayload {
        @JsonView(Visible.class)
        @JsonEnum(Status.class)
        public int status = 1;
    }

    public static class Visible {
    }

    public static class Hidden {
    }

    public static class PolymorphicPayload {
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.WRAPPER_ARRAY)
        public Object value;

        public PolymorphicPayload() {
        }

        PolymorphicPayload(Object value) {
            this.value = value;
        }
    }

    public static final class CachedPayload {
        public final Long id;
        public final LocalDate date;
        public final LocalDateTime timestamp;

        @JsonCreator
        public CachedPayload(@JsonProperty("id") Long id, @JsonProperty("date") LocalDate date,
                             @JsonProperty("timestamp") LocalDateTime timestamp) {
            this.id = id;
            this.date = date;
            this.timestamp = timestamp;
        }
    }

    public enum Status {
        ZERO(0, "Zero"), ONE(1, "One");

        private final int code;
        private final String name;

        Status(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
