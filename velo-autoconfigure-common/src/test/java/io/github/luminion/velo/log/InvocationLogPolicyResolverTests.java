package io.github.luminion.velo.log;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.LogIgnore;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;

class InvocationLogPolicyResolverTests {
    private final VeloProperties properties = new VeloProperties();
    private final InvocationLogPolicyResolver resolver = new InvocationLogPolicyResolver(properties);

    @Test
    void theSameInterfaceMethodUsesEachTargetClassesAnnotations() throws Exception {
        Method method = Contract.class.getMethod("call", String.class);
        LogInvocation active = invocation(method, ActiveService.class);
        LogInvocation ignored = invocation(method, IgnoredService.class);

        assertThat(resolver.resolve(active).policies.get(InvocationLogFeature.ENTRY_ARGS).level)
                .isEqualTo(LogLevel.DEBUG);
        assertThat(resolver.resolve(ignored).policies).isEmpty();
        assertThat(resolver.resolve(active).policies.get(InvocationLogFeature.ENTRY_ARGS).level)
                .isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void missingTargetClassFallsBackToTheDeclaringClass() throws Exception {
        Method method = ActiveService.class.getMethod("call", String.class);

        InvocationLogPolicyResolver.Selection selection = resolver.resolve(invocation(method, null));

        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).level)
                .isEqualTo(LogLevel.DEBUG);
        assertThat(selection.metadata.parameterNames).containsExactly("input");
    }

    @Test
    void cachedMetadataDoesNotFreezeGlobalOrSourceConfiguration() throws Exception {
        LogInvocation invocation = invocation(Contract.class.getMethod("call", String.class), Contract.class);

        assertThat(resolver.resolve(invocation).policies.get(InvocationLogFeature.ENTRY_ARGS).enabled)
                .isTrue();
        properties.getLog().getSources().getInvoke().getEntryArgs().setEnabled(false);
        assertThat(resolver.resolve(invocation).policies.get(InvocationLogFeature.ENTRY_ARGS).enabled)
                .isFalse();
        properties.getLog().setEnabled(false);
        assertThat(resolver.resolve(invocation).policies).isEmpty();
    }

    private LogInvocation invocation(Method method, Class<?> targetClass) {
        return LogInvocation.builder()
                .method(method)
                .targetClass(targetClass)
                .source(InvocationLogSource.INVOKE)
                .build();
    }

    public interface Contract {
        String call(String input);
    }

    public static class ActiveService implements Contract {
        @Override
        @EntryArgs(level = LogLevel.DEBUG)
        public String call(String input) {
            return input;
        }
    }

    @LogIgnore
    public static class IgnoredService implements Contract {
        @Override
        public String call(String input) {
            return input;
        }
    }
}
