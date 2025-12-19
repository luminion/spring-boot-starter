package io.github.luminion.autoconfigure.aop.spi.signature;

import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 基于 SpEL (Spring表达式语言) 的方法签名处理器实现。
 *
 * @author luminion
 */
public class SpelAllArgsSignatureProvider extends SpelSignatureProvider {
    
    public SpelAllArgsSignatureProvider(String prefix) {
        super(prefix);
    }

    @Override
    public String signature(Object target, Method method, Object[] args, String expression) {
        String signature = super.signature(target, method, args, expression);
        StringBuilder keyBuilder = new StringBuilder(signature);
        // 如果表达式为空，则拼接所有参数作为键的一部分
        if (StringUtils.hasText(expression) &&  args != null && args.length > 0 ) {
            keyBuilder.append(':');
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    keyBuilder.append(',');
                }
                keyBuilder.append(args[i]);
            }
        }
        return keyBuilder.toString();
    }

}
