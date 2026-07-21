package io.github.luminion.velo.log.aspect;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.InvocationLogRecord;
import io.github.luminion.velo.log.InvocationLogWriter;
import io.github.luminion.velo.log.InvocationPhase;
import io.github.luminion.velo.log.annotation.InvokeLog;
import io.github.luminion.velo.log.annotation.LogPayloadIgnore;
import io.github.luminion.velo.spi.RuntimeJsonSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvokeLogAspectTest {

    @Test
    void shouldCaptureFinishArgsForNormalAndExceptionalCompletion() {
        RuntimeJsonSerializer serializer = value -> {
            Map<?, ?> arguments = (Map<?, ?>) value;
            MutableArgument argument = (MutableArgument) arguments.values().iterator().next();
            return "{\"value\":\"" + argument.value + "\"}";
        };
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("runtimeJsonSerializer", serializer);
        CapturingInvocationLogWriter writer = new CapturingInvocationLogWriter();
        InvokeLogAspect aspect = new InvokeLogAspect(new VeloProperties(),
                beanFactory.getBeanProvider(RuntimeJsonSerializer.class), writer);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new DemoService());
        proxyFactory.setInterfaces(DemoOperations.class);
        proxyFactory.addAspect(aspect);
        DemoOperations proxy = proxyFactory.getProxy();

        MutableArgument successfulArgument = new MutableArgument("before");
        proxy.complete(successfulArgument);

        assertThat(writer.records).hasSize(2);
        assertThat(writer.records.get(0).getPhase()).isEqualTo(InvocationPhase.ENTRY);
        assertThat(writer.records.get(0).getArgs()).isEqualTo("{\"value\":\"before\"}");
        assertThat(writer.records.get(1).getPhase()).isEqualTo(InvocationPhase.EXIT);
        assertThat(writer.records.get(1).getArgs()).isEqualTo("{\"value\":\"complete\"}");
        assertThat(writer.records.get(1).getResult()).isEqualTo("void");

        MutableArgument failedArgument = new MutableArgument("before");
        assertThatThrownBy(() -> proxy.fail(failedArgument))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(writer.records).hasSize(4);
        assertThat(writer.records.get(3).getPhase()).isEqualTo(InvocationPhase.EXIT);
        assertThat(writer.records.get(3).getArgs()).isEqualTo("{\"value\":\"failed\"}");
        assertThat(writer.records.get(3).getError()).isInstanceOf(IllegalStateException.class);

        MutableArgument ignoredArgument = new MutableArgument("secret");
        assertThat(proxy.ignore(ignoredArgument)).isSameAs(ignoredArgument);

        assertThat(writer.records).hasSize(6);
        assertThat(writer.records.get(4).getArgs()).isEqualTo("ignored");
        assertThat(writer.records.get(5).getResult()).isEqualTo("ignored");
    }

    interface DemoOperations {

        void complete(MutableArgument argument);

        void fail(MutableArgument argument);

        MutableArgument ignore(MutableArgument argument);
    }

    static class DemoService implements DemoOperations {

        @Override
        @InvokeLog(argsOnFinish = true)
        public void complete(MutableArgument argument) {
            argument.value = "complete";
        }

        @Override
        @InvokeLog(argsOnFinish = true)
        public void fail(MutableArgument argument) {
            argument.value = "failed";
            throw new IllegalStateException("boom");
        }

        @Override
        @InvokeLog
        @LogPayloadIgnore
        public MutableArgument ignore(MutableArgument argument) {
            return argument;
        }
    }

    static class MutableArgument {

        private String value;

        MutableArgument(String value) {
            this.value = value;
        }
    }

    static class CapturingInvocationLogWriter implements InvocationLogWriter {

        private final List<InvocationLogRecord> records = new ArrayList<>();

        @Override
        public void write(InvocationLogRecord record) {
            records.add(record);
        }
    }
}
