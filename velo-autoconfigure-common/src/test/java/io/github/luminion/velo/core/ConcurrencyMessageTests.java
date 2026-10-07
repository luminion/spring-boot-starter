package io.github.luminion.velo.core;

import io.github.luminion.velo.VeloProperties;
import io.github.luminion.velo.idempotent.IdempotentHandler;
import io.github.luminion.velo.idempotent.VeloIdempotentAutoConfiguration;
import io.github.luminion.velo.idempotent.annotation.Idempotent;
import io.github.luminion.velo.idempotent.aspect.IdempotentAspect;
import io.github.luminion.velo.idempotent.exception.IdempotentException;
import io.github.luminion.velo.lock.LockHandler;
import io.github.luminion.velo.lock.VeloLockAutoConfiguration;
import io.github.luminion.velo.lock.annotation.Lock;
import io.github.luminion.velo.lock.aspect.LockAspect;
import io.github.luminion.velo.lock.exception.LockException;
import io.github.luminion.velo.ratelimit.RateLimitHandler;
import io.github.luminion.velo.ratelimit.VeloRateLimitAutoConfiguration;
import io.github.luminion.velo.ratelimit.annotation.RateLimit;
import io.github.luminion.velo.ratelimit.aspect.RateLimitAspect;
import io.github.luminion.velo.ratelimit.exception.RateLimitException;
import io.github.luminion.velo.spi.fingerprint.SpelFingerprinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.env.MapPropertySource;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通过自动配置和真实代理验证消息继承、局部覆盖及国际化。
 */
class ConcurrencyMessageTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VeloCoreAutoConfiguration.class,
                    VeloLockAutoConfiguration.class, VeloIdempotentAutoConfiguration.class,
                    VeloRateLimitAutoConfiguration.class))
            .withBean(LockHandler.class, () -> lockHandler(false))
            .withBean(IdempotentHandler.class, () -> (key, token, ttl) -> false)
            .withBean(RateLimitHandler.class, () -> (key, qps) -> false);

    @Test
    void unconfiguredMessagesShouldKeepExistingChineseDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            MessageService service = messageService(context);
            assertThatThrownBy(service::lockDefault).isInstanceOf(LockException.class)
                    .hasMessage("系统繁忙，请稍后再试");
            assertThatThrownBy(service::idempotentDefault).isInstanceOf(IdempotentException.class)
                    .hasMessage("您的请求已提交，请勿重复操作");
            assertThatThrownBy(service::rateDefault).isInstanceOf(RateLimitException.class)
                    .hasMessage("当前访问人数较多，请稍后再试");
        });
    }

    @Test
    void emptyAndWhitespaceAnnotationsShouldInheritConfiguredMessages() {
        configuredRunner().run(context -> {
            assertThat(context).hasNotFailed();
            MessageService service = messageService(context);
            assertThatThrownBy(service::lockDefault).hasMessage("configured lock");
            assertThatThrownBy(service::lockBlank).hasMessage("configured lock");
            assertThatThrownBy(service::idempotentDefault).hasMessage("configured idempotent");
            assertThatThrownBy(service::idempotentBlank).hasMessage("configured idempotent");
            assertThatThrownBy(service::rateDefault).hasMessage("configured rate");
            assertThatThrownBy(service::rateBlank).hasMessage("configured rate");
        });
    }

    @Test
    void explicitAnnotationsShouldOverrideConfigurationEvenWhenEqualToOldDefaults() {
        configuredRunner().run(context -> {
            MessageService service = messageService(context);
            assertThatThrownBy(service::lockOverride).hasMessage("系统繁忙，请稍后再试");
            assertThatThrownBy(service::idempotentOverride).hasMessage("您的请求已提交，请勿重复操作");
            assertThatThrownBy(service::rateOverride).hasMessage("当前访问人数较多，请稍后再试");
        });
    }

    @Test
    void configuredAndAnnotationKeysShouldResolveUsingCurrentLocaleAtRejection() {
        StaticMessageSource source = new StaticMessageSource();
        for (String feature : new String[]{"lock", "idempotent", "rate"}) {
            for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.CHINESE}) {
                source.addMessage("test." + feature + ".config", locale,
                        feature + " config " + locale.getLanguage());
                source.addMessage("test." + feature + ".annotation", locale,
                        feature + " annotation " + locale.getLanguage());
            }
        }
        contextRunner.withBean("messageSource", StaticMessageSource.class, () -> source)
                .withPropertyValues("velo.lock.message={test.lock.config}",
                        "velo.idempotent.message={test.idempotent.config}",
                        "velo.rate-limit.message={test.rate.config}")
                .run(context -> {
                    MessageService service = messageService(context);
                    LocaleContext previousLocale = LocaleContextHolder.getLocaleContext();
                    try {
                        // 同一切面先后使用不同请求语言，避免启动时提前解析并缓存文案。
                        for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.CHINESE}) {
                            LocaleContextHolder.setLocale(locale);
                            String language = locale.getLanguage();
                            assertThatThrownBy(service::lockDefault).hasMessage("lock config " + language);
                            assertThatThrownBy(service::idempotentDefault).hasMessage("idempotent config " + language);
                            assertThatThrownBy(service::rateDefault).hasMessage("rate config " + language);
                            assertThatThrownBy(service::lockLocalized).hasMessage("lock annotation " + language);
                            assertThatThrownBy(service::idempotentLocalized).hasMessage("idempotent annotation " + language);
                            assertThatThrownBy(service::rateLocalized).hasMessage("rate annotation " + language);
                        }
                    } finally {
                        LocaleContextHolder.setLocaleContext(previousLocale);
                    }
                });
    }

    @Test
    void missingConfiguredTranslationsShouldKeepExistingKeyFallback() {
        contextRunner.withPropertyValues("velo.lock.message={missing.lock}",
                        "velo.idempotent.message={missing.idempotent}",
                        "velo.rate-limit.message={missing.rate}")
                .run(context -> {
                    MessageService service = messageService(context);
                    assertThatThrownBy(service::lockDefault).hasMessage("missing.lock");
                    assertThatThrownBy(service::idempotentDefault).hasMessage("missing.idempotent");
                    assertThatThrownBy(service::rateDefault).hasMessage("missing.rate");
                });
    }

    @Test
    void methodRateAnnotationShouldInheritGlobalMessageWhenOverridingClassAnnotation() {
        configuredRunner().run(context -> {
            ClassRateService service = createProxy(new ClassRateService(), context.getBean(RateLimitAspect.class));
            assertThatThrownBy(service::classMessage).hasMessage("class message");
            assertThatThrownBy(service::methodMessage).hasMessage("configured rate");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void explicitBlankConfigurationShouldStartAndKeepUserMessage(String blank) {
        Map<String, Object> messages = new LinkedHashMap<>();
        messages.put("velo.lock.message", blank);
        messages.put("velo.idempotent.message", blank);
        messages.put("velo.rate-limit.message", blank);
        // 属性源直接提供原始值，避免测试属性字符串解析器裁剪空白。
        contextRunner.withInitializer(context -> context.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("blankMessages", messages)))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MessageService service = messageService(context);
                    assertThatThrownBy(service::lockDefault).hasMessage(blank);
                    assertThatThrownBy(service::idempotentDefault).hasMessage(blank);
                    assertThatThrownBy(service::rateDefault).hasMessage(blank);
                });
    }

    @Test
    void legacyAspectConstructorsWithoutResolverShouldKeepDefaultMessages() {
        SpelFingerprinter fingerprinter = new SpelFingerprinter();
        MessageService service = createProxy(new MessageService(),
                new LockAspect("lock:", fingerprinter, lockHandler(false)),
                new IdempotentAspect("idempotent:", fingerprinter, (key, token, ttl) -> false),
                new RateLimitAspect("rateLimit:", fingerprinter, (key, qps) -> false));
        assertThatThrownBy(service::lockDefault).hasMessage(new VeloProperties.LockProperties().getMessage());
        assertThatThrownBy(service::idempotentDefault).hasMessage(new VeloProperties.IdempotentProperties().getMessage());
        assertThatThrownBy(service::rateDefault).hasMessage(new VeloProperties.RateLimitProperties().getMessage());
    }

    @Test
    void acceptedCallsShouldNotResolveMessages() {
        VeloMessageResolver resolver = new VeloMessageResolver() {
            @Override
            public String resolve(String message) {
                throw new AssertionError("Accepted call must not resolve a rejection message.");
            }
        };
        SpelFingerprinter fingerprinter = new SpelFingerprinter();
        AcceptedService service = createProxy(new AcceptedService(),
                new LockAspect("lock:", fingerprinter, lockHandler(true), resolver),
                new IdempotentAspect("idempotent:", fingerprinter, (key, token, ttl) -> true, resolver),
                new RateLimitAspect("rateLimit:", fingerprinter, (key, qps) -> true, resolver));
        assertThat(service.execute()).isEqualTo("accepted");
    }

    private ApplicationContextRunner configuredRunner() {
        return contextRunner.withPropertyValues("velo.lock.message=configured lock",
                "velo.idempotent.message=configured idempotent", "velo.rate-limit.message=configured rate");
    }

    private static MessageService messageService(ApplicationContext context) {
        return createProxy(new MessageService(), context.getBean(LockAspect.class),
                context.getBean(IdempotentAspect.class), context.getBean(RateLimitAspect.class));
    }

    private static <T> T createProxy(T target, Object... aspects) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.setProxyTargetClass(true);
        for (Object aspect : aspects) {
            factory.addAspect(aspect);
        }
        return factory.getProxy();
    }

    private static LockHandler lockHandler(boolean accepted) {
        return new LockHandler() {
            @Override
            public boolean tryLock(String key) {
                return accepted;
            }

            @Override
            public void unlock(String key) {
            }
        };
    }

    static class MessageService {
        @Lock(prefix = "messages")
        public void lockDefault() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Lock(prefix = "messages", message = " \t\n")
        public void lockBlank() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Lock(prefix = "messages", message = "系统繁忙，请稍后再试")
        public void lockOverride() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Lock(prefix = "messages", message = "{test.lock.annotation}")
        public void lockLocalized() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Idempotent(prefix = "messages")
        public void idempotentDefault() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Idempotent(prefix = "messages", message = " \t\n")
        public void idempotentBlank() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Idempotent(prefix = "messages", message = "您的请求已提交，请勿重复操作")
        public void idempotentOverride() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @Idempotent(prefix = "messages", message = "{test.idempotent.annotation}")
        public void idempotentLocalized() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @RateLimit(prefix = "messages")
        public void rateDefault() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @RateLimit(prefix = "messages", message = " \t\n")
        public void rateBlank() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @RateLimit(prefix = "messages", message = "当前访问人数较多，请稍后再试")
        public void rateOverride() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @RateLimit(prefix = "messages", message = "{test.rate.annotation}")
        public void rateLocalized() {
            throw new AssertionError("Rejected call must not execute business.");
        }
    }

    @RateLimit(prefix = "messages", message = "class message")
    static class ClassRateService {
        public void classMessage() {
            throw new AssertionError("Rejected call must not execute business.");
        }

        @RateLimit(prefix = "messages")
        public void methodMessage() {
            throw new AssertionError("Rejected call must not execute business.");
        }
    }

    static class AcceptedService {
        @Lock(prefix = "messages")
        @Idempotent(prefix = "messages")
        @RateLimit(prefix = "messages")
        public String execute() {
            return "accepted";
        }
    }
}
