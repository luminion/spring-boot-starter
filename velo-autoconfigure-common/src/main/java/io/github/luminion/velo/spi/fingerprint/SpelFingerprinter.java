package io.github.luminion.velo.spi.fingerprint;

import io.github.luminion.velo.spi.Fingerprinter;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 SpEL 的键解析器。
 */
public class SpelFingerprinter implements Fingerprinter {
    private static final ParameterNameDiscoverer PND = new DefaultParameterNameDiscoverer();
    private final ExpressionParser parser;
    // 仅缓存注解声明的固定表达式，不缓存求值上下文或请求结果。
    private final ConcurrentMap<String, Expression> expressions = new ConcurrentHashMap<>();
    // 按实际用户类隔离方法标识；ClassValue 随类卸载回收，不持有全局强引用。
    private final ClassValue<ConcurrentMap<Method, String>> methodFingerprints =
            new ClassValue<ConcurrentMap<Method, String>>() {
                @Override
                protected ConcurrentMap<Method, String> computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };

    public SpelFingerprinter() {
        this(new SpelExpressionParser());
    }

    SpelFingerprinter(ExpressionParser parser) {
        this.parser = parser;
    }

    @Override
    public String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression) {
        Class<?> targetClass = target == null ? method.getDeclaringClass()
                : ClassUtils.getUserClass(AopUtils.getTargetClass(target));
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        String methodFingerprint = methodFingerprints.get(targetClass).computeIfAbsent(specificMethod,
                resolvedMethod -> buildMethodFingerprint(targetClass, resolvedMethod));
        if (StringUtils.hasText(expression)) {
            Expression parsedExp = expressions.computeIfAbsent(expression, parser::parseExpression);
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, specificMethod, args, PND);
            Object value = parsedExp.getValue(context);
            if (value == null) {
                throw new IllegalArgumentException("SpEL key expression '" + expression + "' resolved to null.");
            }
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean
                    || value instanceof Character || value instanceof UUID || value instanceof Enum<?>)) {
                throw new IllegalArgumentException("SpEL key expression '" + expression
                        + "' must resolve to a scalar value, but got " + value.getClass().getName() + ".");
            }
            String resolved = value instanceof Enum<?> ? ((Enum<?>) value).name() : value.toString();
            if (!StringUtils.hasText(resolved)) {
                throw new IllegalArgumentException("SpEL key expression '" + expression + "' resolved to a blank value.");
            }
            return methodFingerprint + ':' + resolved;
        }
        return methodFingerprint;
    }

    private static String buildMethodFingerprint(Class<?> targetClass, Method method) {
        StringBuilder fingerprint = new StringBuilder(targetClass.getName())
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
