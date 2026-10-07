package io.github.luminion.velo.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.trace.VeloTraceAutoConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class VeloBannerPrinterTest {

    @Test
    void shouldDisplayCacheSwitchWithoutAnIndependentTtlSetting() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getBanner().setEnabled(true);
        assertThat(printBanner(properties)).contains("cache        on").doesNotContain("ttl=");
        properties.getCache().setEnabled(false);
        assertThat(printBanner(properties)).contains("cache        off");
    }

    @Test
    void shouldSkipBannerWhenBannerConfigurationIsMissing() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.setBanner(null);

        assertThat(printBanner(properties)).isEmpty();
    }

    @Test
    void shouldRenderMissingNestedConfigurationWithoutThrowing() throws Exception {
        VeloProperties properties = new VeloProperties();
        properties.getBanner().setEnabled(true);
        properties.setIdempotent(null);
        properties.getLog().setTrace(null);
        properties.setXss(null);

        String banner = printBanner(properties);

        assertThat(banner)
                .contains("opinionated=true")
                .contains("idempotent   unavailable (config missing)")
                .contains("trace        unavailable (config missing)")
                .contains("xss          unavailable (config missing)");
    }

    @Test
    void independentTraceStateIsDisplayedEvenWhenLoggingIsDisabled(CapturedOutput output) {
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                VeloCoreAutoConfiguration.class, VeloTraceAutoConfiguration.class))
                .withPropertyValues(
                        "velo.banner.enabled=true",
                        "velo.log.enabled=false",
                        "velo.log.trace.enabled=true",
                        "velo.trace.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
        assertThat(output.getOut()).contains("trace        off", "log          off");
    }

    private String printBanner(VeloProperties properties) throws Exception {
        VeloBannerPrinter printer =
                new VeloBannerPrinter(properties, emptyProvider(), emptyProvider(), emptyProvider());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8.name()));
            printer.afterSingletonsInstantiated();
        } finally {
            System.setOut(original);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
