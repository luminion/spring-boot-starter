package io.github.luminion.velo.log.aspect;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.composed.InvokeLog;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.model.InvocationLogFeature;
import io.github.luminion.velo.log.model.InvocationLogRecord;
import io.github.luminion.velo.log.model.InvocationLogSource;
import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

/**
 * 验证耗时日志策略的继承、独立启用和关闭行为。
 */
class CostLogAspectTests {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void classAndMethodCostPoliciesApplyThroughActualProxy(boolean classProxy) {
        VeloProperties properties = new VeloProperties();
        List<InvocationLogRecord> records = new ArrayList<>();
        AspectJProxyFactory factory = new AspectJProxyFactory(new Service());
        factory.setInterfaces(Operations.class);
        factory.setProxyTargetClass(classProxy);
        factory.addAdvisor(new InvokeLogAspect(new InvocationLogEngine(properties, value -> {
            throw new AssertionError("Cost must not serialize an object payload.");
        }, records::add)));
        Operations proxy = factory.getProxy();

        assertThat(proxy.inherited()).isEqualTo("inherited");
        assertThat(records).isEmpty();
        assertThat(proxy.explicit()).isEqualTo("explicit");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.COST_LOG);
            assertThat(record.getLevel()).isEqualTo(LogPolicyLevel.DEBUG);
            assertThat(record.getContent()).matches("\\d+ms");
        });
        records.clear();
        assertThat(proxy.defaults()).isEqualTo("defaults");
        assertThat(records).singleElement().satisfies(record ->
                assertThat(record.getLevel()).isEqualTo(LogPolicyLevel.INFO));
        records.clear();
        proxy.off();
        assertThat(records).isEmpty();
        properties.getLog().getSources().getAnnotation().setEnabled(false);
        proxy.explicit();
        assertThat(records).isEmpty();
        properties.getLog().getSources().getAnnotation().setEnabled(true);
        properties.getLog().setEnabled(false);
        proxy.explicit();
        assertThat(records).isEmpty();
    }

    @Test
    void costAnnotationAloneActivatesOnlyCostLogging() {
        List<InvocationLogRecord> records = new ArrayList<>();
        AspectJProxyFactory factory = new AspectJProxyFactory(new UnmarkedService());
        factory.addAdvisor(new InvokeLogAspect(new InvocationLogEngine(new VeloProperties(),
                String::valueOf, records::add)));
        UnmarkedService proxy = factory.getProxy();
        assertThat(proxy.execute()).isEqualTo("done");
        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getFeature()).isEqualTo(InvocationLogFeature.COST_LOG);
            assertThat(record.getSource()).isEqualTo(InvocationLogSource.ANNOTATION);
        });
    }

    interface Operations {
        String inherited();
        String explicit();
        String defaults();
        void off();
    }

    @CostLog(level = LogPolicyLevel.OFF)
    static class Service implements Operations {
        @Override
        public String inherited() {
            return "inherited";
        }

        @Override
        @CostLog(level = LogPolicyLevel.DEBUG)
        public String explicit() {
            return "explicit";
        }

        @Override
        @CostLog
        public String defaults() {
            return "defaults";
        }

        @Override
        @CostLog(level = LogPolicyLevel.OFF)
        public void off() {
        }
    }

    static class UnmarkedService {
        @CostLog
        public String execute() {
            return "done";
        }
    }
}
