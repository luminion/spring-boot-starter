package io.github.luminion.velo.log.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.log.annotation.CostLog;
import io.github.luminion.velo.log.annotation.LogPolicyLevel;
import io.github.luminion.velo.log.annotation.EntryArgs;
import io.github.luminion.velo.log.annotation.ExitArgs;
import io.github.luminion.velo.log.annotation.ExitResult;
import io.github.luminion.velo.log.annotation.ErrorLog;
import io.github.luminion.velo.log.annotation.SlowLog;
import io.github.luminion.velo.log.annotation.RequestHeadersLog;
import io.github.luminion.velo.log.annotation.ResponseHeadersLog;
import io.github.luminion.velo.log.annotation.LogIgnore;
import io.github.luminion.velo.log.annotation.InvokeLog;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.LogLevel;

/** 验证调用日志策略解析的目标类选择、元数据缓存和配置继承。 */
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
        properties.getLog().getSources().getAnnotation().getEntryArgs().setEnabled(false);
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
        assertThat(explicit.policies.get(InvocationLogFeature.COST_LOG).level).isEqualTo(LogLevel.TRACE);
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
    void concreteAnnotationsEnableFeaturesButInheritUnspecifiedParameters() throws Exception {
        properties.getLog().getDefaults().getSlowLog().setThreshold(0L);
        properties.getLog().getDefaults().getCostLog().setEnabled(false);
        properties.getLog().getSources().getAnnotation().getCostLog().setLevel(LogLevel.ERROR);
        properties.getLog().getSources().getAnnotation().getEntryArgs().setEnabled(false);
        properties.getLog().getSources().getAnnotation().getEntryArgs().setLevel(LogLevel.ERROR);
        properties.getLog().getSources().getAnnotation().getRequestHeaders().setAllowlist(
                java.util.Collections.singletonList("X-Global"));
        InvocationLogPolicyResolver.Selection selection = resolver.resolve(
                invocation(NamedService.class.getMethod("defaults"), NamedService.class));
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).threshold).isZero();
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).level).isEqualTo(LogLevel.WARN);
        assertThat(selection.policies.get(InvocationLogFeature.COST_LOG).enabled).isTrue();
        assertThat(selection.policies.get(InvocationLogFeature.COST_LOG).level).isEqualTo(LogLevel.ERROR);
        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).enabled).isTrue();
        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).level).isEqualTo(LogLevel.ERROR);
        assertThat(selection.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist).containsExactly("X-Global");
    }

    public static class NamedService {
        @CostLog(level = LogPolicyLevel.TRACE)
        @SlowLog(threshold = 17)
        @EntryArgs(level = LogPolicyLevel.DEBUG)
        @ExitArgs(level = LogPolicyLevel.TRACE)
        @ExitResult(level = LogPolicyLevel.WARN)
        @ErrorLog(level = LogPolicyLevel.ERROR)
        @RequestHeadersLog(allowlist = "X-Trace-Id")
        @ResponseHeadersLog(allowlist = "X-Request-Id")
        public String explicit() { return "ok"; }

        @CostLog
        @SlowLog
        @EntryArgs
        @ExitArgs
        @ExitResult
        @ErrorLog
        @RequestHeadersLog
        @ResponseHeadersLog
        public String defaults() { return "ok"; }
    }

    @Test
    void costMethodOverridesClassAndClassOverridesSource() throws Exception {
        properties.getLog().getSources().getAnnotation().getCostLog().setEnabled(true);
        InvocationLogPolicyResolver.Selection inherited = resolver.resolve(
                invocation(CostService.class.getMethod("inherited"), CostService.class));
        assertThat(inherited.policies.get(InvocationLogFeature.COST_LOG).enabled).isFalse();
        InvocationLogPolicyResolver.Selection explicit = resolver.resolve(
                invocation(CostService.class.getMethod("explicit"), CostService.class));
        assertThat(explicit.policies.get(InvocationLogFeature.COST_LOG).enabled).isTrue();
        assertThat(explicit.policies.get(InvocationLogFeature.COST_LOG).level).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void singleFeatureDoesNotSelectOtherGloballyEnabledFeatures() throws Exception {
        properties.getLog().getSources().getAnnotation().getExitArgs().setEnabled(true);
        InvocationLogPolicyResolver.Selection selection = resolver.resolve(
                invocation(SingleCost.class.getMethod("call"), SingleCost.class));
        assertThat(selection.policies.entrySet()).filteredOn(entry -> entry.getValue().enabled)
                .extracting(java.util.Map.Entry::getKey).containsExactly(InvocationLogFeature.COST_LOG);
    }

    @Test
    void profileKeepsConfigurationForEveryFeatureAndDoesNotForceMetaAnnotationValues() throws Exception {
        properties.getLog().getSources().getAnnotation().getExitArgs().setEnabled(true);
        properties.getLog().getSources().getAnnotation().getEntryArgs().setEnabled(false);
        properties.getLog().getSources().getAnnotation().getSlowLog().setThreshold(3000L);
        properties.getLog().getSources().getAnnotation().getSlowLog().setLevel(LogLevel.DEBUG);
        InvocationLogPolicyResolver.Selection selection = resolver.resolve(
                invocation(Profile.class.getMethod("call"), Profile.class));
        assertThat(selection.policies.get(InvocationLogFeature.EXIT_ARGS).enabled).isTrue();
        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).enabled).isFalse();
        assertThat(selection.policies.get(InvocationLogFeature.REQUEST_HEADERS).enabled).isFalse();
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).threshold).isEqualTo(3000L);
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).level).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void methodChangesOnlyExplicitParametersAndReenablesTheFeature() throws Exception {
        InvocationLogPolicyResolver.Selection selection = resolver.resolve(
                invocation(Layered.class.getMethod("partial"), Layered.class));
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).enabled).isTrue();
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).level).isEqualTo(LogLevel.DEBUG);
        assertThat(selection.policies.get(InvocationLogFeature.SLOW_LOG).threshold).isEqualTo(2000L);
        assertThat(selection.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist).containsExactly("X-Class");
        assertThat(selection.policies.get(InvocationLogFeature.REQUEST_HEADERS).level).isEqualTo(LogLevel.TRACE);
        assertThat(selection.policies.get(InvocationLogFeature.ENTRY_ARGS).enabled).isTrue();
    }

    @Test
    void explicitEmptyAllowlistClearsClassRestrictionAndNamesReplaceIt() throws Exception {
        InvocationLogPolicyResolver.Selection all = resolver.resolve(
                invocation(Layered.class.getMethod("allHeaders"), Layered.class));
        assertThat(all.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist).isEmpty();
        InvocationLogPolicyResolver.Selection replaced = resolver.resolve(
                invocation(Layered.class.getMethod("someHeaders"), Layered.class));
        assertThat(replaced.policies.get(InvocationLogFeature.REQUEST_HEADERS).allowlist)
                .containsExactly("X-Method");
    }

    @Test
    void explicitLevelCanOverrideConfiguredOffWithoutLosingEnabledState() throws Exception {
        properties.getLog().getSources().getAnnotation().getCostLog().setLevel(LogLevel.OFF);
        InvocationLogPolicyResolver.Selection selection = resolver.resolve(
                invocation(CostService.class.getMethod("explicit"), CostService.class));
        assertThat(selection.policies.get(InvocationLogFeature.COST_LOG).enabled).isTrue();
        assertThat(selection.policies.get(InvocationLogFeature.COST_LOG).level).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void taskUsesItsOwnDefaultsUnlessTheProfileIsExplicitlySelected() throws Exception {
        LogInvocation plain = invocation(Plain.class.getMethod("call"), Plain.class).toBuilder()
                .source(InvocationLogSource.SCHEDULED).build();
        assertThat(resolver.resolve(plain).policies.get(InvocationLogFeature.ENTRY_ARGS).enabled).isFalse();
        assertThat(resolver.resolve(plain).policies.get(InvocationLogFeature.COST_LOG).enabled).isTrue();
        LogInvocation profile = invocation(Profile.class.getMethod("call"), Profile.class).toBuilder()
                .source(InvocationLogSource.SCHEDULED).build();
        assertThat(resolver.resolve(profile).policies.get(InvocationLogFeature.ENTRY_ARGS).enabled).isTrue();
    }

    @Test
    void invalidThresholdAndMixedAllowlistMarkerAreRejected() throws Exception {
        LogInvocation threshold = invocation(Invalid.class.getMethod("threshold"), Invalid.class);
        assertThatThrownBy(() -> resolver.resolve(threshold)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SlowLog threshold");
        LogInvocation headers = invocation(Invalid.class.getMethod("headers"), Invalid.class);
        assertThatThrownBy(() -> resolver.resolve(headers)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inheritance marker");
    }

    public static class SingleCost {
        @CostLog
        public String call() { return "ok"; }
    }

    @InvokeLog
    public static class Profile {
        public String call() { return "ok"; }
    }

    public static class Plain {
        public String call() { return "ok"; }
    }

    @EntryArgs
    @SlowLog(enabled = false, level = LogPolicyLevel.WARN, threshold = 2000)
    @RequestHeadersLog(allowlist = "X-Class")
    public static class Layered {
        @SlowLog(level = LogPolicyLevel.DEBUG)
        @RequestHeadersLog(level = LogPolicyLevel.TRACE)
        public String partial() { return "ok"; }

        @RequestHeadersLog(allowlist = {})
        public String allHeaders() { return "ok"; }

        @RequestHeadersLog(allowlist = "X-Method")
        public String someHeaders() { return "ok"; }
    }

    public static class Invalid {
        @SlowLog(threshold = -2)
        public String threshold() { return "ok"; }

        @RequestHeadersLog(allowlist = {"", "X-Test"})
        public String headers() { return "ok"; }
    }

    @CostLog(enabled = false)
    public static class CostService {
        public String inherited() { return "ok"; }

        @CostLog(level = LogPolicyLevel.DEBUG)
        public String explicit() { return "ok"; }
    }

    private LogInvocation invocation(Method method, Class<?> targetClass) {
        return LogInvocation.builder()
                .method(method)
                .targetClass(targetClass)
                .source(InvocationLogSource.ANNOTATION)
                .build();
    }

    @InvokeLog
    public interface Contract {
        String call(String input);
    }

    public static class ActiveService implements Contract {
        @Override
        @EntryArgs(level = LogPolicyLevel.DEBUG)
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
