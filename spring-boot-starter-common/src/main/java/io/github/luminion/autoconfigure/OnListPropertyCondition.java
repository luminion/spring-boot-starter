package io.github.luminion.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class OnListPropertyCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        AnnotationAttributes attrs = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ConditionalOnListProperty.class.getName())
        );
        if (attrs == null) {
            return ConditionOutcome.noMatch("No @ConditionalOnListProperty found");
        }

        String name = attrs.getString("value");
        if (!StringUtils.hasText(name)) {
            return ConditionOutcome.noMatch("@ConditionalOnListProperty 'value' must not be empty");
        }

        boolean matchIfEmpty = attrs.getBoolean("matchIfEmpty");
        boolean matchIfMissing = attrs.getBoolean("matchIfMissing");
        Class<?> elementType = attrs.getClass("elementType");
        boolean ignoreEmptyElements = attrs.getBoolean("ignoreEmptyElements");

        ConditionMessage.Builder message = ConditionMessage.forCondition(ConditionalOnListProperty.class, name);

        Binder binder = Binder.get(context.getEnvironment());
        Bindable<? extends List<?>> listBindable = Bindable.listOf(elementType);
        BindResult<? extends List<?>> result = binder.bind(name, listBindable);
        if (!result.isBound()) {
            return matchIfMissing
                    ? ConditionOutcome.match(message.because("property is missing and matchIfMissing=true"))
                    : ConditionOutcome.noMatch(message.because("property is missing"));
        }

        List<?> list = result.get();
        if (list == null) {
            // 按空列表处理
            return matchIfEmpty
                    ? ConditionOutcome.match(message.because("list is null/empty and matchIfEmpty=true"))
                    : ConditionOutcome.noMatch(message.because("list is null/empty"));
        }

        if (ignoreEmptyElements) {
            list = list.stream()
                    .filter(e -> {
                        if (e == null) return false;
                        if (e instanceof CharSequence) return StringUtils.hasText(e.toString());
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        if (list.isEmpty()) {
            return matchIfEmpty
                    ? ConditionOutcome.match(message.because("list is empty and matchIfEmpty=true"))
                    : ConditionOutcome.noMatch(message.because("list is empty"));
        }

        return ConditionOutcome.match(message.because("non-empty list (" + list.size() + " element(s))"));
    }
}