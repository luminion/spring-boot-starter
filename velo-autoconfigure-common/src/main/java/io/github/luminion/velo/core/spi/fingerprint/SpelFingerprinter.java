package io.github.luminion.velo.core.spi.fingerprint;

import io.github.luminion.velo.core.spi.Fingerprinter;
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
import java.util.function.Function;

/**
 * 基于 SpEL 的键解析器。
 */
public class SpelFingerprinter implements Fingerprinter {
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PND = new DefaultParameterNameDiscoverer();
    private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>(64);

    public SpelFingerprinter() {
    }

    @Deprecated
    public SpelFingerprinter(Function<Object[], String> ignored) {
        this();
    }

    @Override
    public String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression) {
        if (StringUtils.hasText(expression)) {
            Expression parsedExp = EXPRESSION_CACHE.computeIfAbsent(expression, PARSER::parseExpression);
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, method, args, PND);
            Object value = parsedExp.getValue(context);
            String resolved = ObjectUtils.nullSafeToString(value);
            if (!StringUtils.hasText(resolved)) {
                throw new IllegalArgumentException("SpEL key expression '" + expression + "' resolved to a blank value.");
            }
            return resolved.trim();
        }
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }
}
