package io.github.luminion.velo.condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

import java.util.Arrays;

/**
 * Velo Redis 模板候选条件。
 *
 * @author luminion
 * @since 1.3.1
 */
public class OnVeloRedisTemplateCondition extends SpringBootCondition {

    private static final Logger log = LoggerFactory.getLogger(OnVeloRedisTemplateCondition.class);

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ConditionalOnVeloRedisTemplate.class.getName())
        );
        if (attrs == null) {
            return ConditionOutcome.noMatch("No @ConditionalOnVeloRedisTemplate found");
        }

        String typeName = attrs.getString("type");
        String fallbackBeanName = attrs.getString("fallbackBeanName");
        Class<?> templateType;
        try {
            templateType = ClassUtils.resolveClassName(typeName, context.getClassLoader());
        } catch (IllegalArgumentException ex) {
            return ConditionOutcome.noMatch("Redis template type " + typeName + " is not present");
        }

        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        String[] candidateNames = beanFactory == null
                ? new String[0]
                : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, templateType, true, false);
        ConditionMessage.Builder message = ConditionMessage.forCondition(
                ConditionalOnVeloRedisTemplate.class, typeName
        );

        if (candidateNames.length == 0) {
            return ConditionOutcome.noMatch(message.because("no candidate bean is present"));
        }
        if (candidateNames.length == 1) {
            return ConditionOutcome.match(message.because("single candidate bean " + candidateNames[0]));
        }

        int primaryCount = countPrimaryBeans(beanFactory, candidateNames);
        if (primaryCount == 1) {
            return ConditionOutcome.match(message.because("one @Primary candidate bean is present"));
        }
        if (primaryCount == 0 && Arrays.asList(candidateNames).contains(fallbackBeanName)) {
            return ConditionOutcome.match(message.because("using fallback bean " + fallbackBeanName));
        }

        log.warn("[Velo Starter] Redis template auto-configuration skipped for {} because multiple " +
                        "candidate beans are present but no unique @Primary or fallback bean can be selected: {}",
                typeName, Arrays.toString(candidateNames));
        return ConditionOutcome.noMatch(message.because("multiple candidate beans are ambiguous: " +
                Arrays.toString(candidateNames)));
    }

    private int countPrimaryBeans(ConfigurableListableBeanFactory beanFactory, String[] candidateNames) {
        if (beanFactory == null) {
            return 0;
        }
        int primaryCount = 0;
        for (String candidateName : candidateNames) {
            if (isPrimaryBean(beanFactory, candidateName)) {
                primaryCount++;
            }
        }
        return primaryCount;
    }

    private boolean isPrimaryBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
        if (beanFactory.containsBeanDefinition(beanName)) {
            return beanFactory.getBeanDefinition(beanName).isPrimary();
        }
        BeanFactory parentBeanFactory = beanFactory.getParentBeanFactory();
        return parentBeanFactory instanceof ConfigurableListableBeanFactory
                && isPrimaryBean((ConfigurableListableBeanFactory) parentBeanFactory, beanName);
    }
}
