package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class VeloPropertiesWarningLoggerTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class));

    @Test
    void shouldNotWarnForDefaultProperties() {
        assertThat(VeloPropertiesWarningLogger.collectWarnings(new VeloProperties())).isEmpty();
    }

    @Test
    void shouldSkipWarningsForDisabledFeatures() {
        VeloProperties properties = new VeloProperties();
        properties.getCache().setEnabled(false);
        properties.getCache().setDefaultTtl(Duration.ZERO);
        properties.getLog().setEnabled(false);
        properties.getLog().getTrace().setHeaderName("");
        properties.getWeb().getCors().setEnabled(false);
        properties.getWeb().getCors().setMaxAge(-1);
        properties.getJackson().setEnabled(false);
        properties.getJackson().setEnumNameSuffix("");
        properties.getExcel().setEnabled(false);
        properties.getSpringConverter().setDateTimeEnabled(false);
        properties.getDateTimeFormat().setDate("invalid[");
        properties.getDateTimeFormat().setTimeZone("invalid-zone");

        assertThat(VeloPropertiesWarningLogger.collectWarnings(properties)).isEmpty();
    }

    @Test
    void shouldWarnForInvalidEnabledFeatureProperties() {
        VeloProperties properties = new VeloProperties();
        properties.getIdempotent().setPrefix("");
        properties.getRateLimit().setPrefix(" ");
        properties.getLock().setPrefix("");
        properties.getLock().setRetryInterval(Duration.ZERO);
        properties.getCache().setDefaultTtl(Duration.ZERO);
        properties.getCache().setSeparator(" ");
        properties.getCache().getTtl().put("orders", Duration.ofSeconds(-1));
        properties.getLog().getTrace().setHeaderName("");
        properties.getLog().getTrace().setMdcKey(" ");
        properties.getLog().getInvocation().setMaxPayloadLength(-2);
        properties.getJackson().setEnumNameSuffix("");
        properties.getJackson().getEnumMappings().put("", "name");
        properties.getWeb().getCors().setEnabled(true);
        properties.getWeb().getCors().setAllowedOriginPatterns(new String[0]);
        properties.getWeb().getCors().setAllowedMethods(new String[]{""});
        properties.getWeb().getCors().setMaxAge(-1);
        properties.getDateTimeFormat().setDate("invalid[");
        properties.getDateTimeFormat().setTimeZone("invalid-zone");

        List<String> warnings = VeloPropertiesWarningLogger.collectWarnings(properties);

        assertThat(warnings)
                .anySatisfy(warning -> assertThat(warning).contains("velo.idempotent.prefix"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.rate-limit.prefix"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.lock.retry-interval"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.cache.default-ttl"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.cache.ttl[orders]"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.log.trace.header-name"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.log.invocation.max-payload-length"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.jackson.enum-name-suffix"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.jackson.enum-mappings"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.web.cors.allowed-origin-patterns"))
                .anySatisfy(warning -> assertThat(warning).contains("velo.web.cors.max-age"))
                .anySatisfy(warning -> assertThat(warning)
                        .contains("velo.date-time-format.date", "启动时失败"))
                .anySatisfy(warning -> assertThat(warning)
                        .contains("velo.date-time-format.time-zone", "启动时失败"));
    }

    @Test
    void shouldLogWarningWithoutPreventingApplicationStartup(CapturedOutput output) {
        contextRunner
                .withPropertyValues("velo.cache.default-ttl=0s")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNull();
                    assertThat(context).hasSingleBean(VeloPropertiesWarningLogger.class);
                });

        assertThat(output.getOut())
                .contains("[Velo Starter] 配置告警")
                .contains("velo.cache.default-ttl");
    }
}
