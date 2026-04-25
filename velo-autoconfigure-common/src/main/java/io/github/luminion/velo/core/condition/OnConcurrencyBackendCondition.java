package io.github.luminion.velo.core.condition;

import io.github.luminion.velo.core.ConcurrencyBackend;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class OnConcurrencyBackendCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ConditionalOnConcurrencyBackend.class.getName())
        );
        if (attrs == null) {
            return ConditionOutcome.noMatch("No @ConditionalOnConcurrencyBackend found");
        }

        String prefix = attrs.getString("prefix");
        if (!StringUtils.hasText(prefix)) {
            return ConditionOutcome.noMatch("@ConditionalOnConcurrencyBackend 'prefix' must not be empty");
        }

        ConcurrencyBackend backend = attrs.getEnum("value");
        ConditionMessage.Builder message = ConditionMessage.forCondition(
                ConditionalOnConcurrencyBackend.class, prefix + ".backend", backend.name()
        );

        Binder binder = Binder.get(context.getEnvironment());
        ConcurrencyBackend configured = binder.bind(prefix + ".backend", ConcurrencyBackend.class)
                .orElse(ConcurrencyBackend.AUTO);
        if (configured != ConcurrencyBackend.AUTO) {
            return configured == backend
                    ? ConditionOutcome.match(message.because("backend is explicitly " + backend.name()))
                    : ConditionOutcome.noMatch(message.because("backend is explicitly " + configured.name()));
        }

        boolean matchAuto = attrs.getBoolean("matchAuto");
        if (!matchAuto) {
            return ConditionOutcome.noMatch(message.because("backend is AUTO and matchAuto=false"));
        }

        String[] autoClassNames = attrs.getStringArray("autoClassNames");
        for (String className : autoClassNames) {
            if (!ClassUtils.isPresent(className, context.getClassLoader())) {
                return ConditionOutcome.noMatch(message.because(className + " is not present"));
            }
        }

        String[] autoBeanNames = attrs.getStringArray("autoBeanNames");
        for (String beanName : autoBeanNames) {
            if (!containsBean(context.getBeanFactory(), beanName)) {
                return ConditionOutcome.noMatch(message.because("bean named " + beanName + " is not present"));
            }
        }

        String[] autoBeanTypeNames = attrs.getStringArray("autoBeanTypeNames");
        for (String beanTypeName : autoBeanTypeNames) {
            if (!containsBeanOfType(context.getBeanFactory(), context.getClassLoader(), beanTypeName)) {
                return ConditionOutcome.noMatch(message.because("bean of type " + beanTypeName + " is not present"));
            }
        }

        return ConditionOutcome.match(message.because("backend is AUTO and requirements are met"));
    }

    private boolean containsBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
        return beanFactory != null && (beanFactory.containsBeanDefinition(beanName) || beanFactory.containsBean(beanName));
    }

    private boolean containsBeanOfType(ConfigurableListableBeanFactory beanFactory, ClassLoader classLoader,
            String beanTypeName) {
        if (beanFactory == null) {
            return false;
        }
        Class<?> beanType;
        try {
            beanType = ClassUtils.resolveClassName(beanTypeName, classLoader);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType, true, false);
        return beanNames.length > 0;
    }
}
