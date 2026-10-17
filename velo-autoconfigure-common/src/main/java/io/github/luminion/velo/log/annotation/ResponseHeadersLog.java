package io.github.luminion.velo.log.annotation;

import io.github.luminion.velo.log.model.LogPolicyLevel;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 记录入口提供的 HTTP 响应头。
 * 每条声明包含完整默认值，不从其他声明逐项继承属性。
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseHeadersLog {
    /**
     * 本功能使用的日志级别，OFF 关闭该功能。
     */
    LogPolicyLevel level() default LogPolicyLevel.INFO;

    /**
     * 最终正文的最大字符数；-1 不截断，0 不输出本项正文日志。
     */
    int maxLength() default -1;

    /**
     * 允许记录的头名称，忽略大小写；空数组表示全部头。
     */
    String[] allowlist() default {};
}
