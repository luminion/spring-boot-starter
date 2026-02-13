package io.github.luminion.starter.core.fingerprint;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 SpEL 的键解析器
 */
public class SpelMethodFingerprinter implements MethodFingerprinter {
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PND = new DefaultParameterNameDiscoverer();
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>(64);

    @Override
    public String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression) {
        // 2. 优化 Key 结构：使用 "类名#方法名"，比 toGenericString 更短更清晰
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(method.getDeclaringClass().getName())
                .append('#')
                .append(method.getName());

        // 情况 A：配置了 SpEL 表达式
        if (StringUtils.hasText(expression)) {
            // 从缓存获取或解析
            Expression parsedExp = EXPRESSION_CACHE.computeIfAbsent(expression, PARSER::parseExpression);

            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, method, args, PND);
            Object value = parsedExp.getValue(context);

            keyBuilder.append(':').append(ObjectUtils.nullSafeToString(value));
            return keyBuilder.toString();
        }
        //
        // 情况 B：没配表达式，使用所有参数拼接
        if (args != null && args.length > 0) {
            keyBuilder.append(':');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    keyBuilder.append(',');
                }
                keyBuilder.append(ObjectUtils.nullSafeToString(args[i]));
            }
        }

        return keyBuilder.toString();
    }
}