package io.github.luminion.velo.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 全部调用日志功能的可配置默认套餐；初始启闭与 Controller 相同，具体功能注解优先。 */
@EntryArgs
@ExitArgs(enabled = false)
@ExitResult
@CostLog
@SlowLog
@ErrorLog
@RequestHeadersLog(enabled = false)
@ResponseHeadersLog(enabled = false)
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvokeLog {}
