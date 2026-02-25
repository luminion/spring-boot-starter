package io.github.luminion.starter.core.util;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.ObjectUtils;

import java.util.stream.IntStream;

/**
 * AOP 相关工具类
 *
 * @author luminion
 * @since 1.0.0
 */
public class AspectUtils {

    /**
     * 获取方法简洁名称 (类简名.方法名)
     *
     * @param signature 方法签名
     * @return 方法名称
     */
    public static String getMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 获取方法全限定名 (类全名.方法名)
     *
     * @param signature 方法签名
     * @return 全限定方法名
     */
    public static String getFullMethodName(MethodSignature signature) {
        return signature.getDeclaringType().getName() + "." + signature.getName();
    }

    /**
     * 格式化方法入参
     *
     * @param signature 方法签名
     * @param args      实际参数值
     * @return 格式化后的参数字符串
     */
    public static String getFormatArgs(MethodSignature signature, Object[] args) {
        String[] paramNames = signature.getParameterNames();
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        IntStream.range(0, args.length).forEach(i -> {
            String paramName = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            sb.append(paramName).append("=").append(args[i]);
            if (i < args.length - 1) {
                sb.append(", ");
            }
        });
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取方法参数的简单字符串标识
     *
     * @param args args
     * @return 字符串
     */
    public static String getArgsSimpleString(Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();
        if (args != null && args.length > 0) {
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
