package io.github.luminion.velo.log.aspect;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 验证定时任务单次及重复调度时的调用日志行为。
 */
class ScheduledLogAspectTests {
    @Test
    void shouldLogSingleAndRepeatedSchedulesThroughActualProxy() {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getSources().getScheduled().setDefaultAnnotation(TimingPreset.class);
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogEngine engine = new InvocationLogEngine(properties, String::valueOf, records::add);
        AspectJProxyFactory factory = new AspectJProxyFactory(new Tasks());
        factory.addAspect(new ScheduledLogAspect(properties, engine));
        Tasks tasks = factory.getProxy();
        tasks.single();
        tasks.repeated();
        assertThat(records).hasSize(4).allSatisfy(record ->
                assertThat(record.getSource()).isEqualTo(InvocationLogSource.SCHEDULED));
        assertThat(records).extracting(InvocationLogRecord::getFeature)
                .containsExactly(InvocationLogFeature.COST_LOG, InvocationLogFeature.SLOW_LOG,
                        InvocationLogFeature.COST_LOG, InvocationLogFeature.SLOW_LOG);
    }

    static class Tasks {
        @Scheduled(fixedDelay = 1000)
        public void single() {
        }
        @Scheduled(fixedDelay = 1000)
        @Scheduled(fixedDelay = 2000)
        public void repeated() {
        }
    }
    @Retention(RetentionPolicy.RUNTIME)
    @CostLog
    @SlowLog(threshold = 0)
    @interface TimingPreset {
    }
}
