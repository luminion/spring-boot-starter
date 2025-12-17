package io.github.luminion.autoconfigure.aop.spi;

import java.lang.reflect.Method;

/**
 * 方法调用唯一签名处理器接口。
 * <p>
 * 定义了如何为一个方法的调用生成一个唯一的“签名”或“键”。
 *
 * @author luminion
 */
public interface SignatureProvider {

    /**
     * 根据方法调用信息生成一个唯一的签名字符串。
     *
     * @param target     方法调用的目标对象
     * @param method     被调用的方法
     * @param args       传递给方法的参数
     * @param expression 用于计算签名的表达式 (例如, SpEL)
     * @return 一个代表该方法调用的唯一 {@link String} 签名
     */
    String signature(Object target, Method method, Object[] args, String expression);

}
