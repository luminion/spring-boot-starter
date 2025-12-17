package io.github.luminion.autoconfigure.aop.spi.signature;

import io.github.luminion.autoconfigure.aop.spi.SignatureProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 基于 SpEL (Spring表达式语言) 的方法签名处理器实现。
 *
 * @author luminion
 */
@RequiredArgsConstructor
public class SpelSignatureProvider implements SignatureProvider {
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PND = new DefaultParameterNameDiscoverer();

    /**
     * 签名（锁键）的前缀。
     */
    private final String prefix;

    @Override
    public String signature(Object target, Method method, Object[] args, String expression) {
        String methodString = method.toGenericString();
        StringBuilder keyBuilder = new StringBuilder(prefix).append(methodString);
        // 如果SpEL表达式不为空，则解析表达式并附加结果
        if (StringUtils.hasText(expression)) {
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, method, args, PND);
            Object value = PARSER.parseExpression(expression).getValue(context);
            keyBuilder.append(':').append(value);
            return keyBuilder.toString();
        }
        return keyBuilder.toString();
    }

}
