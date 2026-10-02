package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.aspect.ScheduledLogAspect;
import io.github.luminion.velo.log.aspect.XxlJobLogAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class VeloSourceLogAutoConfigurationTests {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    VeloCoreAutoConfiguration.class,
                                    VeloLogAutoConfiguration.class,
                                    VeloSourceLogAutoConfiguration.class));

    @Test
    void enablesScheduledByDefaultAndSkipsAbsentXxlJobLibrary() {
        runner
                .withClassLoader(new FilteredClassLoader("com.xxl.job"))
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(ScheduledLogAspect.class);
                            assertThat(context).doesNotHaveBean(XxlJobLogAspect.class);
                        });
    }

    @Test
    void sourceSwitchDisablesScheduledAdapter() {
        runner
                .withPropertyValues("velo.log.sources.scheduled.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ScheduledLogAspect.class));
    }

    @Test
    void globalSwitchDisablesTaskAdapters() {
        runner
                .withPropertyValues("velo.log.enabled=false")
                .run(
                        context -> {
                            assertThat(context).doesNotHaveBean(ScheduledLogAspect.class);
                            assertThat(context).doesNotHaveBean(XxlJobLogAspect.class);
                        });
    }
}
