package io.github.luminion.velo;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class VeloPropertiesDefaultsTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class));

    @Test
    void shouldUseProductionSafeDefaultsForLogging() {
        VeloProperties properties = new VeloProperties();

        assertThat(properties.isOpinionated()).isTrue();
        assertThat(properties.getLog().getDefaults().getEntryArgs().getLevel()).isNull();
        assertThat(properties.getLog().getDefaults().getSlowLog().getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(properties.getSpringConverter().isDateTimeEnabled()).isTrue();
        assertThat(properties.getExcel().getConverters().isEnabled()).isTrue();
        assertThat(properties.getJackson().isSerializeLongAsString()).isTrue();
        assertThat(properties.getJackson().isSerializeBigDecimalAsString()).isTrue();
        assertThat(properties.getJackson().isBigDecimalStripTrailingZeros()).isFalse();
        assertThat(properties.getJackson().isSerializeFloatingAsString()).isFalse();
        assertThat(properties.getJackson().isDateTimeEnabled()).isTrue();
        assertThat(properties.getJackson().isEnumDescEnabled()).isTrue();
        assertThat(properties.getJackson().getEnumNameSuffix()).isEqualTo("name");
        assertThat(properties.getJackson().getEnumMappings())
                .containsEntry("code", "name")
                .containsEntry("key", "value");
        assertThat(properties.getIdempotent().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getIdempotent().getPrefix()).isEqualTo("idempotent:");
        assertThat(properties.getIdempotent().getMessage()).isEqualTo("您的请求已提交，请勿重复操作");
        assertThat(properties.getRateLimit().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getRateLimit().getPrefix()).isEqualTo("rateLimit:");
        assertThat(properties.getRateLimit().getMessage()).isEqualTo("当前访问人数较多，请稍后再试");
        assertThat(properties.getLock().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getLock().getPrefix()).isEqualTo("lock:");
        assertThat(properties.getLock().getMessage()).isEqualTo("系统繁忙，请稍后再试");
        assertThat(properties.getLock().getRedisTtlSeconds()).isEqualTo(60);
        assertThat(properties.getCache().isEnabled()).isTrue();
        assertThat(properties.getCache().getSeparator()).isEqualTo(":");
        assertThat(properties.getCache().isTransactionAware()).isFalse();
        assertThat(properties.getCache().getTtlJitterPercentage()).isEqualTo(0);
        assertThat(properties.getLog().isEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().isEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().getMdcKey()).isEqualTo("traceId");
        assertThat(properties.getLog().getTrace().isFeignPropagationEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().isLoggingPatternEnabled()).isTrue();
        assertThat(properties.getLog().getSources().getController().isEnabled()).isTrue();
        assertThat(properties.getLog().getSources().getFeign().isEnabled()).isTrue();
        assertThat(properties.getLog().getDefaults().getMaxPayloadLength()).isEqualTo(-1);
        assertThat(properties.getLog().getDefaults().getSlowLog().getThreshold()).isEqualTo(1000L);
        assertThat(properties.getAspectOrder().getIdempotent())
                .isLessThan(properties.getAspectOrder().getRateLimit());
        assertThat(properties.getAspectOrder().getRateLimit())
                .isLessThan(properties.getAspectOrder().getLock());
        assertThat(properties.getAspectOrder().getLock())
                .isLessThan(properties.getAspectOrder().getInvokeLog());
        assertThat(properties.getAspectOrder().getInvokeLog())
                .isLessThan(properties.getAspectOrder().getControllerLog());
        assertThat(properties.getAspectOrder().getControllerLog())
                .isLessThan(properties.getAspectOrder().getFeignLog());
        assertThat(properties.getWeb().isEnabled()).isTrue();
        assertThat(properties.getWeb().getCors().isEnabled()).isFalse();
        assertThat(properties.getWeb().getCors().isAllowCredentials()).isFalse();
        assertThat(properties.getXss().getStrategy())
                .isEqualTo(io.github.luminion.velo.xss.XssStrategy.NONE);
        assertThat(properties.getXss().isWebEnabled()).isTrue();
        assertThat(properties.getXss().isJacksonEnabled()).isFalse();
        assertThat(properties.getFeign().isEnabled()).isTrue();
    }

    @Test
    void shouldBindNonInvasiveFlag() {
        contextRunner
                .withPropertyValues("velo.opinionated=false")
                .run(
                        context -> assertThat(context.getBean(VeloProperties.class).isOpinionated()).isFalse());
    }

    @Test
    void shouldIgnoreRemovedLegacyModeProperty() {
        contextRunner
                .withPropertyValues("velo.mode=CONSERVATIVE")
                .run(context -> assertThat(context.getBean(VeloProperties.class).isOpinionated()).isTrue());
    }

    @Test
    void shouldIgnoreRemovedLegacyCorsProperty() {
        contextRunner
                .withPropertyValues("velo.web.allow-cors=true")
                .run(
                        context -> {
                            VeloProperties properties = context.getBean(VeloProperties.class);

                            assertThat(properties.getWeb().getCors().isEnabled()).isFalse();
                        });
    }

    @Test
    void shouldMergeDefaultEnumMappingsWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "velo.jackson.enum-mappings.key=name", "velo.jackson.enum-mappings.value=desc")
                .run(
                        context -> {
                            VeloProperties properties = context.getBean(VeloProperties.class);

                            assertThat(properties.getJackson().getEnumMappings())
                                    .containsEntry("code", "name")
                                    .containsEntry("key", "name")
                                    .containsEntry("value", "desc");
                        });
    }

    @Test
    void shouldRejectOutOfRangeCacheTtlJitterPercentage() {
        contextRunner
                .withPropertyValues("velo.cache.ttl-jitter-percentage=101")
                .run(
                        context ->
                                assertThat(context.getStartupFailure())
                                        .isNotNull()
                                        .hasRootCauseMessage("Cache TTL jitter percentage must be between 0 and 100."));
    }
}
