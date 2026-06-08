package io.github.luminion.velo.log;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.aspect.InvokeLogAspect;
import io.github.luminion.velo.log.aspect.SlowLogAspect;
import io.github.luminion.velo.log.support.Slf4JInvocationLogWriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class VeloLogAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloLogAutoConfiguration.class
            ));

    @Test
    void shouldCreateUnifiedInvocationLoggingBeansByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InvocationLogWriter.class);
            assertThat(context.getBean(InvocationLogWriter.class)).isInstanceOf(Slf4JInvocationLogWriter.class);
            assertThat(context).hasSingleBean(InvokeLogAspect.class);
            assertThat(context).hasSingleBean(SlowLogAspect.class);
        });
    }

    @Test
    void shouldPreferCustomInvocationLogWriterWhenAvailable() {
        contextRunner
                .withBean(CustomInvocationLogWriter.class, CustomInvocationLogWriter::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(InvocationLogWriter.class);
                    assertThat(context.getBean(InvocationLogWriter.class)).isInstanceOf(CustomInvocationLogWriter.class);
                    assertThat(context).hasSingleBean(InvokeLogAspect.class);
                    assertThat(context).hasSingleBean(SlowLogAspect.class);
                });
    }

    @Test
    void shouldSkipInvocationLoggingBeansWhenInvocationLoggingDisabled() {
        contextRunner
                .withPropertyValues("velo.log.invocation.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(InvocationLogWriter.class);
                    assertThat(context).doesNotHaveBean(InvokeLogAspect.class);
                    assertThat(context).doesNotHaveBean(SlowLogAspect.class);
                });
    }

    @Test
    void shouldSkipMethodAspectsWhenMethodInvocationLoggingDisabled() {
        contextRunner
                .withPropertyValues("velo.log.invocation.method.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(InvocationLogWriter.class);
                    assertThat(context).doesNotHaveBean(InvokeLogAspect.class);
                    assertThat(context).doesNotHaveBean(SlowLogAspect.class);
                });
    }

    static final class CustomInvocationLogWriter implements InvocationLogWriter {

        @Override
        public void write(InvocationLogRecord record) {
        }
    }
}
