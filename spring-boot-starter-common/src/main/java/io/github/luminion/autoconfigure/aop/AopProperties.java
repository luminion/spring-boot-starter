package io.github.luminion.autoconfigure.aop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * AOP（面向切面编程）相关配置属性
 *
 * @author luminion
 */
@ConfigurationProperties("turbo.aop")
@Data
public class AopProperties {
    /**
     * 是否启用AOP自动配置
     */
    private boolean enabled = true;
    /**
     * 方法限流锁的键名前缀
     */
    private String methodLimitPrefix = "method_limit:";
    
    /**
     * 日志敏感属性字段(记录时进行脱敏)
     */
    private Set<String> logExcludeProperties;
}
