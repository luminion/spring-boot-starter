package io.github.luminion.velo.log;

import io.github.luminion.velo.core.VeloCoreAutoConfiguration;
import io.github.luminion.velo.log.aspect.ArgsLogAspect;
import io.github.luminion.velo.log.aspect.ErrorLogAspect;
import io.github.luminion.velo.log.aspect.ResultLogAspect;
import io.github.luminion.velo.log.aspect.SlowLogAspect;
import io.github.luminion.velo.log.support.Slf4JLogWriter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class VeloLogAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    VeloCoreAutoConfiguration.class,
                    VeloLogAutoConfiguration.class
            ));

    @Test
    void shouldDefaultWriterToInfoLevel() {
        contextRunner.run(context -> {
            Slf4JLogWriter writer = context.getBean(Slf4JLogWriter.class);
            assertThat(ReflectionTestUtils.getField(writer, "level")).isEqualTo(org.slf4j.event.Level.INFO);
        });
    }

    @Test
    void shouldPreferCustomWritersWhenAvailable() {
        contextRunner
                .withBean(CustomLogWriter.class, CustomLogWriter::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(Slf4JLogWriter.class);
                    assertThat(context).hasSingleBean(ArgsLogAspect.class);
                    assertThat(context).hasSingleBean(ResultLogAspect.class);
                    assertThat(context).hasSingleBean(ErrorLogAspect.class);
                    assertThat(context).hasSingleBean(SlowLogAspect.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ArgsLogAspect.class), "argsWriter"))
                            .isInstanceOf(CustomLogWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ResultLogAspect.class), "resultWriter"))
                            .isInstanceOf(CustomLogWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ErrorLogAspect.class), "errorLogWriter"))
                            .isInstanceOf(CustomLogWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlowLogAspect.class), "slowLogWriter"))
                            .isInstanceOf(CustomLogWriter.class);
                });
    }

    @Test
    void shouldKeepDefaultWritersForOtherAspectsWhenSingleCustomWriterProvided() {
        contextRunner
                .withBean(CustomArgsWriter.class, CustomArgsWriter::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(Slf4JLogWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ArgsLogAspect.class), "argsWriter"))
                            .isInstanceOf(CustomArgsWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ResultLogAspect.class), "resultWriter"))
                            .isInstanceOf(Slf4JLogWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(ErrorLogAspect.class), "errorLogWriter"))
                            .isInstanceOf(Slf4JLogWriter.class);
                    assertThat(ReflectionTestUtils.getField(context.getBean(SlowLogAspect.class), "slowLogWriter"))
                            .isInstanceOf(Slf4JLogWriter.class);
                });
    }

    static final class CustomLogWriter implements InvokeArgsWriter, InvokeResultWriter, ErrorLogWriter, SlowLogWriter {

        @Override
        public void writeArgs(org.aspectj.lang.reflect.MethodSignature signature, Object[] args) {
        }

        @Override
        public void writeResult(org.aspectj.lang.reflect.MethodSignature signature, Object result) {
        }

        @Override
        public void writeError(org.aspectj.lang.reflect.MethodSignature signature, Object[] args, Throwable e) {
        }

        @Override
        public void writeSlow(org.aspectj.lang.reflect.MethodSignature signature, long durationNs) {
        }
    }

    static final class CustomArgsWriter implements InvokeArgsWriter {

        @Override
        public void writeArgs(org.aspectj.lang.reflect.MethodSignature signature, Object[] args) {
        }
    }
}
