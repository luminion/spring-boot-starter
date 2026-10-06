package io.github.luminion.velo.spi;

import java.lang.reflect.Method;

/**
 * 方法签名器
 * <p>
 * 定义了如何为一个方法的调用生成一个唯一的“签名”或“键”。
 *
 * @author luminion
 */
@FunctionalInterface
public interface Fingerprinter {

    /**
     * 生成包含实际用户类完整名称、方法名和参数类型的方法标识。
     * 非空表达式的结果作为后缀追加，不替代方法维度，确保不同方法独立。
     *
     * @param target     方法调用的目标对象
     * @param method     被调用的方法
     * @param args       传递给方法的参数
     * @param expression 用于计算签名的表达式 (例如, SpEL)
     * @return 一个代表该方法调用的唯一 {@link String} 签名
     */
    String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression);

}
