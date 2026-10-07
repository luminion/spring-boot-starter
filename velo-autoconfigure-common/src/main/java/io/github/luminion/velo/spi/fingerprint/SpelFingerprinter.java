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
    private final ExpressionParser parser;
    // 参数发现器和表达式都可能缓存反射元数据，统一随实际用户类卸载回收。
    private final ClassValue<ClassMetadata> metadata =
            new ClassValue<ClassMetadata>() {
                @Override
                protected ClassMetadata computeValue(Class<?> type) {
                    return new ClassMetadata();
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
        ClassMetadata classMetadata = metadata.get(targetClass);
        String methodFingerprint = classMetadata.methodFingerprints.computeIfAbsent(specificMethod,
                resolvedMethod -> buildMethodFingerprint(targetClass, resolvedMethod));
        if (StringUtils.hasText(expression)) {
            Expression parsedExp = classMetadata.expressions.computeIfAbsent(expression, parser::parseExpression);
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(target, specificMethod, args,
                    classMetadata.parameterNames);
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

    private static final class ClassMetadata {
        private final ParameterNameDiscoverer parameterNames = new DefaultParameterNameDiscoverer();
        private final ConcurrentMap<Method, String> methodFingerprints = new ConcurrentHashMap<>();
        // 仅缓存注解声明的表达式，不缓存求值上下文或请求结果。
        private final ConcurrentMap<String, Expression> expressions = new ConcurrentHashMap<>();
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
