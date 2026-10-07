package io.github.luminion.velo.web.exception;

import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 为显式注册的异常处理器提供 MVC Advice 元数据。
 *
 * <p>抽象父类不会被组件扫描实例化；具体处理器通过 {@code @Bean} 注册后即可生效。</p>
 */
@RestControllerAdvice
public abstract class AbstractVeloControllerAdvice implements Ordered {
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
