package io.github.luminion.velo.spi.fingerprint;

import io.github.luminion.velo.spi.Fingerprinter;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 基于 SpEL 的键解析器。
 */
public class SpelFingerprinter implements Fingerprinter {
    private static final int EXPRESSION_CACHE_MAX_SIZE = 256;
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PND = new DefaultParameterNameDiscoverer();
    private static final Map<String, Expression> EXPRESSION_CACHE = new LinkedHashMap<String, Expression>(
            EXPRESSION_CACHE_MAX_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Expression> eldest) {
            return size() > EXPRESSION_CACHE_MAX_SIZE;
        }
    };

    public SpelFingerprinter() {
    }

    @Deprecated
    public SpelFingerprinter(Function<Object[], String> ignored) {
        this();
    }

    @Override
    public String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression) {
        if (StringUtils.hasText(expression)) {
            Expression parsedExp;
            synchronized (EXPRESSION_CACHE) {
                parsedExp = EXPRESSION_CACHE.get(expression);
                if (parsedExp == null) {
                    parsedExp = PARSER.parseExpression(expression);
                    EXPRESSION_CACHE.put(expression, parsedExp);
                }
            }
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, method, args, PND);
            Object value = parsedExp.getValue(context);
            if (value == null) {
                throw new IllegalArgumentException("SpEL key expression '" + expression + "' resolved to null.");
            }
            String resolved = ObjectUtils.nullSafeToString(value);
            if (!StringUtils.hasText(resolved)) {
                throw new IllegalArgumentException("SpEL key expression '" + expression + "' resolved to a blank value.");
            }
            return resolved.trim();
        }
        StringBuilder fingerprint = new StringBuilder(method.getDeclaringClass().getName())
                .append('#')
                .append(method.getName())
                .append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                fingerprint.append(',');
            }
            fingerprint.append(parameterTypes[i].getTypeName());
        }
        return fingerprint.append(')').toString();
    }
}
