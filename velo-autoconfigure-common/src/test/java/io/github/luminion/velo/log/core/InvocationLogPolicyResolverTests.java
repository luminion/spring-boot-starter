package io.github.luminion.velo.log.core;


import static org.assertj.core.api.Assertions.assertThat;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.LogIgnore;
import org.springframework.core.annotation.AnnotationConfigurationException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shorthandAndExplicitAttributesResolveToTheSamePolicies() throws Exception {
        InvocationLogPolicyResolver.Selection shorthand = resolver.resolve(
                invocation(AliasedService.class.getMethod("shortform"), AliasedService.class));
        InvocationLogPolicyResolver.Selection explicit = resolver.resolve(
                invocation(AliasedService.class.getMethod("explicit"), AliasedService.class));
        assertThat(shorthand.policies.get(InvocationLogFeature.SLOW_LOG).threshold).isEqualTo(17L);
        assertThat(shorthand.policies.get(InvocationLogFeature.ENTRY_ARGS).level).isEqualTo(LogLevel.DEBUG);
        assertThat(shorthand.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist)
                .containsExactly("X-Trace-Id");
        assertThat(shorthand.policies.get(InvocationLogFeature.RESPONSE_HEADERS).allowlist)
                .containsExactly("X-Request-Id");
        for (InvocationLogFeature feature : shorthand.policies.keySet()) {
            InvocationLogPolicyResolver.FeaturePolicy actual = shorthand.policies.get(feature);
            InvocationLogPolicyResolver.FeaturePolicy expected = explicit.policies.get(feature);
            assertThat(actual.level).isEqualTo(expected.level);
            assertThat(actual.threshold).isEqualTo(expected.threshold);
            assertThat(actual.allowlist).isEqualTo(expected.allowlist);
        }
    }

    @Test
    void conflictingAliasesFailDuringPolicyResolution() throws Exception {
        LogInvocation invocation = invocation(AliasedService.class.getMethod("conflict"), AliasedService.class);
        assertThatThrownBy(() -> resolver.resolve(invocation))
                .isInstanceOf(AnnotationConfigurationException.class);
    }

    public static class AliasedService {
        @SlowLog(17)
        @EntryArgs(LogLevel.DEBUG)
        @ExitArgs(LogLevel.TRACE)
        @ExitResult(LogLevel.WARN)
        @ErrorLog(LogLevel.ERROR)
        @RequestHeadersLog("X-Trace-Id")
        @ResponseHeadersLog("X-Request-Id")
        public String shortform() { return "ok"; }

        @SlowLog(thresholdMs = 17)
        @EntryArgs(level = LogLevel.DEBUG)
        @ExitArgs(level = LogLevel.TRACE)
        @ExitResult(level = LogLevel.WARN)
        @ErrorLog(level = LogLevel.ERROR)
        @RequestHeadersLog(allowlist = "X-Trace-Id")
        @ResponseHeadersLog(allowlist = "X-Request-Id")
        public String explicit() { return "ok"; }

        @SlowLog(value = 17, thresholdMs = 20)
        public String conflict() { return "ok"; }
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
