package io.github.luminion.starter.repeat.aspect;

import io.github.luminion.starter.repeat.annotation.RepeatSubmit;
import io.github.luminion.starter.repeat.exception.RepeatSubmitException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Method;

/**
 * 防重复提交切面
 * <p>
 * 拦截标注了@RepeatSubmit的方法，在方法执行前检查是否为重复提交
 *
 * @author luminion
 * @since 1.0.0
 */
@Aspect
@RequiredArgsConstructor
public class RepeatSubmitAspect {
    private final BeanFactory beanFactory;

    @Before("@annotation(repeatSubmit)")
    public void before(JoinPoint joinPoint, RepeatSubmit repeatSubmit) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        String springExpression = repeatSubmit.value();
        
        // 使用KeyResolver解析唯一签名
        String signature = beanFactory.getBean(repeatSubmit.keyResolver())
                .resolveMethodFingerprint(joinPoint.getTarget(), method, joinPoint.getArgs(), springExpression);
        
        // 使用RepeatSubmitHandler检查是否为重复提交
        boolean isRepeat = beanFactory.getBean(repeatSubmit.handler())
                .isRepeatSubmit(signature, repeatSubmit.timeout());
        
        if (isRepeat) {
            throw new RepeatSubmitException(repeatSubmit.message());
        }
    }

}

