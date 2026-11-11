package io.github.luminion.velo.log.aspect;

import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.Advisor;
import io.github.luminion.velo.core.DeferredInterceptorAdvisor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证日志功能注解的入口选择、继承解析和来源边界。
 */
class AnnotationLogAdvisorTests {
    @Test
    void deferredAdvisorResolvesInterceptorOnceForConcurrentInvocations() throws Exception {
        List<InvocationLogRecord> records = Collections.synchronizedList(new ArrayList<>());
        InvocationLogEngine engine = new InvocationLogEngine(new VeloProperties(), String::valueOf, records::add);
        AtomicInteger resolutions = new AtomicInteger();
        DeferredInterceptorAdvisor advisor = new DeferredInterceptorAdvisor(InvokeLogAspect.pointcut(), 42, () -> {
            resolutions.incrementAndGet();
            return new InvokeLogAspect(engine);
        });
        ProxyFactory factory = new ProxyFactory(new AtomicService());
        factory.setInterfaces(Operations.class);
        factory.addAdvisor(advisor);
        Operations calls = (Operations) factory.getProxy();

        // 读取顺序和创建代理都不解析拦截器。
        assertThat(advisor.getOrder()).isEqualTo(42);
        assertThat(resolutions).hasValue(0);

        int workerCount = 8;
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < workerCount; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return calls.cost("input");
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<String> result : results) {
                assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("input");
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }

        assertThat(resolutions).hasValue(1);
        assertThat(records).hasSize(workerCount);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void everyAtomicAnnotationSelectsOnlyItsOwnFeature(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        ProxyFactory factory = new ProxyFactory(new AtomicService());
        factory.setInterfaces(Operations.class);
        factory.setProxyTargetClass(classProxy);
        factory.addAdvisor(advisor(records));
        Operations calls = (Operations) factory.getProxy();

        assertThat(calls.entry("input")).isEqualTo("input");
        assertOnly(records, InvocationLogFeature.ENTRY_ARGS);
        calls.exit("input");
        assertOnly(records, InvocationLogFeature.EXIT_ARGS);
        calls.result("input");
        assertOnly(records, InvocationLogFeature.EXIT_RESULT);
        calls.cost("input");
        assertOnly(records, InvocationLogFeature.COST_LOG);
        calls.slow("input");
        assertOnly(records, InvocationLogFeature.SLOW_LOG);
        assertThatThrownBy(() -> calls.error("input")).isInstanceOf(IllegalStateException.class)
                .hasMessage("failed");
        assertThat(records).extracting(InvocationLogRecord::getLevel).containsExactly(LogPolicyLevel.WARN);
        assertOnly(records, InvocationLogFeature.ERROR_LOG);
        calls.request("input");
        calls.response("input");
        calls.ignore("input");
        assertThat(records).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void inheritedInterfaceAndClassParametersMatchAndResolveTogether(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        ProxyFactory factory = new ProxyFactory(new ChildService());
        factory.setInterfaces(InheritedOperations.class);
        factory.setProxyTargetClass(classProxy);
        factory.addAdvisor(advisor(records));
        InheritedOperations calls = (InheritedOperations) factory.getProxy();
        calls.call("input");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.SLOW_LOG);
            assertThat(record.getLevel()).isEqualTo(LogPolicyLevel.DEBUG);
            assertThat(record.getContent()).endsWith("threshold=0ms}");
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void automaticSourcesNeverFallBackToAnnotationAdapter(boolean classProxy) {
        List<InvocationLogRecord> records = new ArrayList<>();
        for (Object target : new Object[]{new ControllerService(), new FeignService(), new ScheduledService()}) {
            ProxyFactory factory = new ProxyFactory(target);
            factory.setInterfaces(SourceOperations.class);
            factory.setProxyTargetClass(classProxy);
            factory.addAdvisor(advisor(records));
            ((SourceOperations) factory.getProxy()).call();
        }
        assertThat(records).isEmpty();
    }

    private Advisor advisor(List<InvocationLogRecord> records) {
        InvokeLogAspect aspect =
                new InvokeLogAspect(new InvocationLogEngine(new VeloProperties(), String::valueOf, records::add));
        return new DefaultPointcutAdvisor(InvokeLogAspect.pointcut(), aspect);
    }

    private void assertOnly(List<InvocationLogRecord> records, InvocationLogFeature feature) {
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getFeature()).isEqualTo(feature);
            assertThat(record.getSource()).isEqualTo(InvocationLogSource.ANNOTATION);
        });
        records.clear();
    }

    interface Operations {
        String entry(String input);
        String exit(String input);
        String result(String input);
        String cost(String input);
        String slow(String input);
        String error(String input);
        String request(String input);
        String response(String input);
        String ignore(String input);
    }

    static class AtomicService implements Operations {
        @EntryArgs
        public String entry(String input) { return input; }
        @ExitArgs
        public String exit(String input) { return input; }
        @ExitResult
        public String result(String input) { return input; }
        @CostLog
        public String cost(String input) { return input; }
        @SlowLog(threshold = 0)
        public String slow(String input) { return input; }
        @ErrorLog
        public String error(String input) { throw new IllegalStateException("failed"); }
        @RequestHeadersLog
        public String request(String input) { return input; }
        @ResponseHeadersLog
        public String response(String input) { return input; }
        @LogIgnore
        public String ignore(String input) { return input; }
    }

    interface InheritedOperations {
        @SlowLog(level = LogPolicyLevel.DEBUG, threshold = 0)
        String call(String input);
    }

    @SlowLog(level = LogPolicyLevel.OFF, threshold = 0)
    static class ParentService {
    }

    static class ChildService extends ParentService implements InheritedOperations {
        public String call(String input) { return input; }
    }

    interface SourceOperations {
        String call();
    }

    @RestController
    @InvokeLog
    static class ControllerService implements SourceOperations {
        @CostLog
        public String call() { return "ok"; }
    }

    @FeignClient(name = "demo")
    @InvokeLog
    static class FeignService implements SourceOperations {
        @CostLog
        public String call() { return "ok"; }
    }

    @InvokeLog
    static class ScheduledService implements SourceOperations {
        @Scheduled(fixedDelay = 1000)
        @CostLog
        public String call() { return "ok"; }
    }
}
