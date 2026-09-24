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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 基于 SpEL 的键解析器。
 */
public class SpelFingerprinter implements Fingerprinter {
    private static final int EXPRESSION_CACHE_MAX_SIZE = 256;
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PND = new DefaultParameterNameDiscoverer();
    // 表达式不可变、解析结果线程安全；锁-free 读取，超限时整体清空自愈（防动态表达式洪峰撑爆缓存）
    private static final ConcurrentHashMap<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

    public SpelFingerprinter() {
    }

    @Deprecated
    public SpelFingerprinter(Function<Object[], String> ignored) {
        this();
    }

    @Override
    public String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression) {
        if (StringUtils.hasText(expression)) {
            Expression parsedExp = EXPRESSION_CACHE.get(expression);
            if (parsedExp == null) {
                // 解析在锁外进行：并发重复解析同一表达式是幂等的，最后一次 put 生效即可
                parsedExp = PARSER.parseExpression(expression);
                if (EXPRESSION_CACHE.size() >= EXPRESSION_CACHE_MAX_SIZE) {
                    EXPRESSION_CACHE.clear();
                }
                EXPRESSION_CACHE.put(expression, parsedExp);
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
