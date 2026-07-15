package io.github.luminion.velo;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.logging.LogLevel;

import static org.assertj.core.api.Assertions.assertThat;

class VeloPropertiesDefaultsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class));

    @Test
    void shouldUseProductionSafeDefaultsForLogging() {
        VeloProperties properties = new VeloProperties();

        assertThat(properties.getMode()).isEqualTo(VeloMode.OPINIONATED);
        assertThat(properties.getLog().getLevel()).isEqualTo(LogLevel.INFO);
        assertThat(properties.getLog().getSlow().getLevel()).isEqualTo(LogLevel.WARN);
        assertThat(properties.getSpringConverter().isDateTimeEnabled()).isTrue();
        assertThat(properties.getExcel().getConverters().isEnabled()).isTrue();
        assertThat(properties.getExcel().isEnabled()).isTrue();
        assertThat(properties.getJackson().isSerializeLongAsString()).isTrue();
        assertThat(properties.getJackson().isSerializeBigDecimalAsString()).isTrue();
        assertThat(properties.getJackson().isBigDecimalStripTrailingZeros()).isFalse();
        assertThat(properties.getJackson().isSerializeFloatingAsString()).isFalse();
        assertThat(properties.getJackson().isDateTimeEnabled()).isTrue();
        assertThat(properties.getJackson().isEnumDescEnabled()).isTrue();
        assertThat(properties.getJackson().isStringConverterEnabled()).isTrue();
        assertThat(properties.getJackson().getEnumNameSuffix()).isEqualTo("name");
        assertThat(properties.getJackson().getEnumMappings())
                .containsEntry("code", "name")
                .containsEntry("key", "value");
        assertThat(properties.getIdempotent().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getIdempotent().getPrefix()).isEqualTo("idempotent:");
        assertThat(properties.getRateLimit().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getRateLimit().getPrefix()).isEqualTo("rateLimit:");
        assertThat(properties.getLock().getBackend()).isEqualTo(ConcurrencyBackend.AUTO);
        assertThat(properties.getLock().getPrefix()).isEqualTo("lock:");
        assertThat(properties.getLock().getRetryInterval()).isEqualTo(java.time.Duration.ofMillis(10));
        assertThat(properties.getCache().isEnabled()).isTrue();
        assertThat(properties.getCache().getDefaultTtl()).isEqualTo(java.time.Duration.ofMinutes(5));
        assertThat(properties.getCache().isNullCachingEnabled()).isTrue();
        assertThat(properties.getCache().getTtlJitterPercentage()).isEqualTo(0);
        assertThat(properties.getLog().isEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().isEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().getHeaderName()).isEqualTo("X-Trace-Id");
        assertThat(properties.getLog().getTrace().getMdcKey()).isEqualTo("traceId");
        assertThat(properties.getLog().getTrace().isResponseHeaderEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().isFeignPropagationEnabled()).isTrue();
        assertThat(properties.getLog().getTrace().isLoggingPatternEnabled()).isTrue();
        assertThat(properties.getLog().getInvocation().isEnabled()).isTrue();
        assertThat(properties.getLog().getInvocation().getMaxPayloadLength()).isEqualTo(-1);
        assertThat(properties.getLog().getInvocation().isIncludeArgs()).isTrue();
        assertThat(properties.getLog().getInvocation().isIncludeResult()).isTrue();
        assertThat(properties.getLog().getInvocation().isIncludeErrorStackTrace()).isFalse();
        assertThat(properties.getLog().getInvocation().getController().isEnabled()).isTrue();
        assertThat(properties.getLog().getInvocation().getFeign().isEnabled()).isTrue();
        assertThat(properties.getLog().getInvocation().getMethod().isEnabled()).isTrue();
        assertThat(properties.getAspectOrder().getIdempotent())
                .isLessThan(properties.getAspectOrder().getRateLimit());
        assertThat(properties.getAspectOrder().getRateLimit())
                .isLessThan(properties.getAspectOrder().getLock());
        assertThat(properties.getAspectOrder().getLock())
                .isLessThan(properties.getAspectOrder().getSlowLog());
        assertThat(properties.getAspectOrder().getSlowLog())
                .isLessThan(properties.getAspectOrder().getInvokeLog());
        assertThat(properties.getAspectOrder().getInvokeLog())
                .isLessThan(properties.getAspectOrder().getControllerLog());
        assertThat(properties.getAspectOrder().getControllerLog())
                .isLessThan(properties.getAspectOrder().getFeignLog());
        assertThat(properties.getWeb().isEnabled()).isTrue();
        assertThat(properties.getFeign().isEnabled()).isTrue();
    }

    @Test
    void shouldMergeDefaultEnumMappingsWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "velo.jackson.enum-mappings.key=name",
                        "velo.jackson.enum-mappings.value=desc")
                .run(context -> {
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
                .run(context -> assertThat(context.getStartupFailure())
                        .isNotNull()
                        .hasRootCauseMessage("Cache TTL jitter percentage must be between 0 and 100."));
    }
}
