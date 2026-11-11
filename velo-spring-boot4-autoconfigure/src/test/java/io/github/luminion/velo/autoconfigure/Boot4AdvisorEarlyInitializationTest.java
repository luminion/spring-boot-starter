package io.github.luminion.velo.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.spi.InvocationLogWriter;
import io.github.luminion.velo.log.spi.LogValueFormatter;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证 Starter 的 Advisor 不会在 BeanPostProcessor 注册阶段连带初始化配置属性、日志引擎、ObjectMapper 等依赖。
 *
 * @author luminion
 */
@ExtendWith(OutputCaptureExtension.class)
class Boot4AdvisorEarlyInitializationTest {
    private static final String NOT_ELIGIBLE = "not eligible for getting processed by all BeanPostProcessors";
    private static final Pattern BEAN_NAME = Pattern.compile("Bean '([^']+)'");

    @Test
    void plainPostProcessorDoesNotTriggerNotEligibleWarnings(CapturedOutput output) {
        ConfigurableApplicationContext context =
                start(PlainPostProcessorApplication.class, "--velo.aspect-order.invoke-log=123456");
        try {
            // 顺序由 Advisor 创建时直接读取配置，不依赖配置属性 Bean。
            assertThat(context.getBean("invokeLogAdvisor", DeferredInterceptorAdvisor.class).getOrder())
                    .isEqualTo(123456);
            RecordingPostProcessor recorder = context.getBean(RecordingPostProcessor.class);
            assertThat(recorder.processed(VeloProperties.class)).isTrue();
            assertThat(recorder.processed(InvocationLogEngine.class)).isTrue();
            assertThat(recorder.processed(LogValueFormatter.class)).isTrue();
            assertThat(context.getBean(LoggedService.class).call("ok")).matches("[0-9a-f]{32}:ok");
            assertThat(MDC.get("traceId")).isNull();
        } finally {
            context.close();
        }
        assertThat(notEligibleBeans(output)).isEmpty();
    }

    @Test
    void postProcessorDependingOnAdvisedBeansDoesNotPullStarterDependencies(CapturedOutput output) {
        ConfigurableApplicationContext context = start(DependentPostProcessorApplication.class);
        try {
            // 提前创建的业务 Bean 仍被代理，拦截器在首次调用时解析。
            assertThat(context.getBean(EarlyController.class).hello()).isEqualTo("hello");
            assertThat(context.getBean(RecordingWriter.class).sources).contains(InvocationLogSource.CONTROLLER);
        } finally {
            context.close();
        }
        // 只允许业务方自己被提前依赖的 Bean 出现告警，Starter 不得追加任何依赖链。
        assertThat(notEligibleBeans(output)).isSubsetOf("earlyController", "earlyLockedService");
    }

    private static ConfigurableApplicationContext start(Class<?> source, String... extraArgs) {
        SpringApplication application = new SpringApplication(source);
        application.setWebApplicationType(WebApplicationType.SERVLET);
        application.setRegisterShutdownHook(false);
        List<String> args = new ArrayList<>(Arrays.asList(
                "--spring.main.banner-mode=off", "--velo.banner.enabled=false", "--server.port=0"));
        args.addAll(Arrays.asList(extraArgs));
        return application.run(args.toArray(new String[0]));
    }

    private static List<String> notEligibleBeans(CapturedOutput output) {
        return new BufferedReader(new StringReader(output.getAll())).lines()
                .filter(line -> line.contains(NOT_ELIGIBLE))
                .map(line -> {
                    Matcher matcher = BEAN_NAME.matcher(line);
                    return matcher.find() ? matcher.group(1) : line;
                })
                .collect(Collectors.toList());
    }

    /** 模拟 sentinelBeanPostProcessor 这类没有依赖的普通后置处理器。 */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV4")
    static class PlainPostProcessorApplication {
        @Bean
        static RecordingPostProcessor recordingPostProcessor() {
            return new RecordingPostProcessor();
        }

        @Bean
        static LoggedService loggedService() {
            return new LoggedService();
        }
    }

    /** 后置处理器依赖了会被 Starter 切面命中的业务 Bean。 */
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.redisson.spring.starter.RedissonAutoConfigurationV4")
    static class DependentPostProcessorApplication {
        @Bean
        static DependentPostProcessor dependentPostProcessor(EarlyController controller, EarlyLockedService service) {
            return new DependentPostProcessor();
        }

        @Bean
        static EarlyController earlyController() {
            return new EarlyController();
        }

        @Bean
        static EarlyLockedService earlyLockedService() {
            return new EarlyLockedService();
        }

        @Bean
        static RecordingWriter recordingWriter() {
            return new RecordingWriter();
        }
    }

    static class RecordingPostProcessor implements BeanPostProcessor {
        private final Set<Class<?>> types = new CopyOnWriteArraySet<>();

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            types.add(bean.getClass());
            return bean;
        }

        boolean processed(Class<?> type) {
            return types.stream().anyMatch(type::isAssignableFrom);
        }
    }

    static class DependentPostProcessor implements BeanPostProcessor {
    }

    static class LoggedService {
        @InvokeLog
        public String call(String value) {
            return MDC.get("traceId") + ":" + value;
        }
    }

    @RestController
    static class EarlyController {
        @GetMapping("/early")
        public String hello() {
            return "hello";
        }
    }

    static class EarlyLockedService {
        @Lock
        public void locked() {
        }
    }

    static class RecordingWriter implements InvocationLogWriter {
        private final List<InvocationLogSource> sources = new CopyOnWriteArrayList<>();

        @Override
        public void write(InvocationLogRecord record) {
            sources.add(record.getSource());
        }
    }
}
