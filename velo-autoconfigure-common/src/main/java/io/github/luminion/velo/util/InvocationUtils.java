package io.github.luminion.velo.util;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 调用链路相关的通用工具。
 */
public abstract class InvocationUtils {

    private static final String OMITTED_VALUE = "[omitted]";
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    private static final String[] UNLOGGABLE_TYPE_PREFIXES = {
            "jakarta.servlet.",
            "javax.servlet.",
            "org.springframework.validation.",
            "org.springframework.web.",
            "org.springframework.http.",
            "org.springframework.core.io.",
            "org.springframework.ui.",
            "org.springframework.web.multipart."
    };

    public static String getMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    public static String getFullMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getName() + "." + signature.getName();
    }

    public static String formatArguments(MethodSignature signature, Object[] args, int maxLength) {
        String[] parameterNames = resolveParameterNames(signature);
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            String parameterName = parameterNames != null && i < parameterNames.length ? parameterNames[i] : "arg" + i;
            builder.append(parameterName).append('=').append(formatValue(args[i], maxLength));
        }
        builder.append(']');
        return builder.toString();
    }

    public static String formatValue(Object value, int maxLength) {
        String rendered = renderValue(value);
        if (maxLength <= 0 || rendered.length() <= maxLength) {
            return rendered;
        }
        return rendered.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static String joinArgumentValues(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        List<String> values = new ArrayList<>(args.length);
        for (Object arg : args) {
            String rendered = renderValue(arg);
            if (!rendered.isEmpty()) {
                values.add(rendered);
            }
        }
        return String.join(",", values);
    }

    public static String[] resolveParameterNames(MethodSignature signature) {
        return resolveParameterNames(signature, null);
    }

    public static String[] resolveParameterNames(MethodSignature signature, Object target) {
        Method method = signature.getMethod();
        if (method != null) {
            String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
            if ((parameterNames == null || parameterNames.length == 0) && target != null) {
                Method specificMethod = ClassUtils.getMostSpecificMethod(method, target.getClass());
                if (!specificMethod.equals(method)) {
                    parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(specificMethod);
                }
            }
            if (parameterNames != null && parameterNames.length > 0) {
                return parameterNames;
            }
        }
        return signature.getParameterNames();
    }

    private static String renderValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (!isLoggableValue(value)) {
            return OMITTED_VALUE;
        }
        return ObjectUtils.nullSafeToString(value);
    }

    public static boolean isLoggableValue(Object value) {
        if (value == null) {
            return true;
        }

        Class<?> valueType = value.getClass();
        if (valueType.isPrimitive() || value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?> || value instanceof java.util.Date || value instanceof java.time.temporal.Temporal
                || value instanceof java.util.UUID) {
            return true;
        }
        if (valueType.isArray()) {
            return isLoggableArray(value);
        }
        if (value instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) value;
            return collection.stream().allMatch(InvocationUtils::isLoggableValue);
        }
        if (value instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) value;
            return map.entrySet().stream().allMatch(entry -> isLoggableValue(entry.getKey()) && isLoggableValue(entry.getValue()));
        }
        if (value instanceof InputStream || value instanceof OutputStream || value instanceof Reader || value instanceof Writer
                || value instanceof File || value instanceof Throwable) {
            return false;
        }

        return !hasTypeNamePrefix(valueType);
    }

    private static boolean isLoggableArray(Object value) {
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            if (!isLoggableValue(Array.get(value, i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasTypeNamePrefix(Class<?> valueType) {
        if (valueType == null || valueType == Object.class) {
            return false;
        }
        for (String prefix : UNLOGGABLE_TYPE_PREFIXES) {
            if (valueType.getName().startsWith(prefix)) {
                return true;
            }
        }
        for (Class<?> interfaceType : valueType.getInterfaces()) {
            if (hasTypeNamePrefix(interfaceType)) {
                return true;
            }
        }
        return hasTypeNamePrefix(valueType.getSuperclass());
    }
}
