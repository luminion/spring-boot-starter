package io.github.luminion.velo.spi;

import java.lang.reflect.Method;

/**
 * 并发控制键解析器。
 * <p>
 * 定义了如何为一个方法的调用生成一个唯一的“签名”或“键”。
 *
 * @author luminion
 */
@FunctionalInterface
public interface Fingerprinter {

    /**
     * 生成方法范围或资源范围的键，不包含各功能的全局配置前缀。
     * prefix 为空时使用实际用户类完整名称、方法名和参数类型；非空时以固定 prefix 替代方法指纹。
     * 非空表达式的结果作为后缀追加；表达式为空时不拼接参数。
     *
     * @param target     方法调用的目标对象
     * @param method     被调用的方法
     * @param args       传递给方法的参数
     * @param prefix     固定资源前缀，非 SpEL；为空时使用方法指纹
     * @param expression 用于计算签名的表达式 (例如, SpEL)
     * @return 一个代表该方法调用的唯一 {@link String} 签名
     */
    String resolveMethodFingerprint(Object target, Method method, Object[] args, String prefix, String expression);

    /**
     * 生成默认方法范围的键，等价于传入空资源前缀。
     */
    default String resolveMethodFingerprint(Object target, Method method, Object[] args, String expression) {
        return resolveMethodFingerprint(target, method, args, "", expression);
    }

}
