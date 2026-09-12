package io.github.luminion.velo.xss;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * 仅在用户选择了非 {@link XssStrategy#NONE} 策略时启用 Velo 内置 XSS 清洗器。
 *
 * <p>不能用 {@code @ConditionalOnProperty} 表达“多个非 NONE 枚举值任意一个”，
 * 因此使用条件类避免在默认配置下创建一个实际不做任何处理的 Cleaner Bean。</p>
 */
public class XssStrategyEnabledCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String strategy = context.getEnvironment().getProperty("velo.xss.strategy");
        if (!StringUtils.hasText(strategy) || XssStrategy.NONE.name().equalsIgnoreCase(strategy.trim())) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition("Velo XSS strategy")
                    .because("velo.xss.strategy is missing or NONE"));
        }
        return ConditionOutcome.match(ConditionMessage.forCondition("Velo XSS strategy")
                .because("velo.xss.strategy is enabled"));
    }
}
