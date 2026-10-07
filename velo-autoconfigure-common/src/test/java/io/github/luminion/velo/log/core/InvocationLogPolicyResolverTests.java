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
    void explicitAttributesResolveToExpectedPolicies() throws Exception {
        InvocationLogPolicyResolver.Selection explicit = resolver.resolve(
                invocation(NamedService.class.getMethod("explicit"), NamedService.class));
        assertThat(explicit.policies.get(InvocationLogFeature.SLOW_LOG).threshold).isEqualTo(17L);
        assertThat(explicit.policies.get(InvocationLogFeature.ENTRY_ARGS).level).isEqualTo(LogLevel.DEBUG);
        assertThat(explicit.policies.get(InvocationLogFeature.EXIT_ARGS).level).isEqualTo(LogLevel.TRACE);
        assertThat(explicit.policies.get(InvocationLogFeature.EXIT_RESULT).level).isEqualTo(LogLevel.WARN);
        assertThat(explicit.policies.get(InvocationLogFeature.ERROR_LOG).level).isEqualTo(LogLevel.ERROR);
        assertThat(explicit.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist)
                .containsExactly("X-Trace-Id");
        assertThat(explicit.policies.get(InvocationLogFeature.RESPONSE_HEADERS).allowlist)
                .containsExactly("X-Request-Id");
    }

    @Test
    void annotationDefaultsOverrideConflictingGlobalAndSourceSettings() throws Exception {
        properties.getLog().getDefaults().getSlowLog().setThreshold(0L);
        properties.getLog().getSources().getInvoke().getEntryArgs().setEnabled(false);
        properties.getLog().getSources().getInvoke().getEntryArgs().setLevel(LogLevel.ERROR);
        properties.getLog().getSources().getInvoke().getRequestHeaders().setAllowlist(
                java.util.Collections.singletonList("X-Global"));
        InvocationLogPolicyResolver.Selection selection = resolver.resolve(
                invocation(NamedService.class.getMethod("defaults"), NamedService.class));
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).threshold).isEqualTo(1000L);
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).level).isEqualTo(LogLevel.WARN);
        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).enabled).isTrue();
        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).level).isEqualTo(LogLevel.INFO);
        assertThat(selection.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist).isEmpty();
    }

    public static class NamedService {
        @SlowLog(threshold = 17)
        @EntryArgs(level = LogLevel.DEBUG)
        @ExitArgs(level = LogLevel.TRACE)
        @ExitResult(level = LogLevel.WARN)
        @ErrorLog(level = LogLevel.ERROR)
        @RequestHeadersLog(allowlist = "X-Trace-Id")
        @ResponseHeadersLog(allowlist = "X-Request-Id")
        public String explicit() { return "ok"; }

        @SlowLog
        @EntryArgs
        @ExitArgs
        @ExitResult
        @ErrorLog
        @RequestHeadersLog
        @ResponseHeadersLog
        public String defaults() { return "ok"; }
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
