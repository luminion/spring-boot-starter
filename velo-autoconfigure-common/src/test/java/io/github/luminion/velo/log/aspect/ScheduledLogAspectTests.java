package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogRecord;
import io.github.luminion.velo.log.core.InvocationLogSource;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduledLogAspectTests {
    @Test
    void shouldLogSingleAndRepeatedSchedulesThroughActualProxy() {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getSources().getScheduled().getSlowLog().setThresholdMs(0L);
        List<InvocationLogRecord> records = new ArrayList<>();
        InvocationLogEngine engine = new InvocationLogEngine(properties, String::valueOf, records::add);
        AspectJProxyFactory factory = new AspectJProxyFactory(new Tasks());
        factory.addAspect(new ScheduledLogAspect(properties, engine));
        Tasks tasks = factory.getProxy();
        tasks.single();
        tasks.repeated();
        assertThat(records).hasSize(2).allSatisfy(record ->
                assertThat(record.getSource()).isEqualTo(InvocationLogSource.SCHEDULED));
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
}
