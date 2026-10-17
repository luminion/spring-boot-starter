package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.LogPolicyLevel;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.core.InvocationLogEngine;
import io.github.luminion.velo.log.core.InvocationLogFeature;
import io.github.luminion.velo.log.core.InvocationLogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.logging.LogLevel;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CostLogAspectTests {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void classAndMethodCostPoliciesApplyThroughActualProxy(boolean classProxy) {
        VeloProperties properties = new VeloProperties();
        properties.getLog().getDefaults().setMaxPayloadLength(0);
        properties.getLog().getSources().getAnnotation().getCostLog().setLevel(LogLevel.ERROR);
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
            assertThat(record.getLevel()).isEqualTo(LogLevel.DEBUG);
            assertThat(record.getContent()).matches("\\d+ms");
        });
        records.clear();
        assertThat(proxy.defaults()).isEqualTo("defaults");
        assertThat(records).singleElement().satisfies(record ->
                assertThat(record.getLevel()).isEqualTo(LogLevel.ERROR));
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
            assertThat(record.getSource()).isEqualTo(io.github.luminion.velo.log.core.InvocationLogSource.ANNOTATION);
        });
    }

    interface Operations {
        String inherited();
        String explicit();
        String defaults();
        void off();
    }

    @InvokeLog
    @CostLog(enabled = false)
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
